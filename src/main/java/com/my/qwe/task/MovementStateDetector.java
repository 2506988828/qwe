package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;

import java.io.IOException;

/**
 * 角色移动状态检测器
 * 用于判断角色是否正在前往目的地、是否原地不动，以及是否需要重新激活前往目的地
 */
public class MovementStateDetector {
    private final TaskContext context;
    private final CommonActions commonActions;
    private final GameStateDetector gameStateDetector;

    public MovementStateDetector(TaskContext context, CommonActions commonActions, GameStateDetector gameStateDetector) {
        this.context = context;
        this.commonActions = commonActions;
        this.gameStateDetector = gameStateDetector;
    }

    /**
     * 移动状态枚举
     */
    public enum MovementState {
        MOVING_TO_DESTINATION,    // 正在前往目的地
        STATIONARY,              // 原地不动
        ABNORMAL_MOVEMENT,       // 行动异常，需要重新激活
        IN_BATTLE,               // 正在战斗中
        UNKNOWN                  // 未知状态
    }

    /**
     * 检测角色移动状态
     * @param destinationX 目的地X坐标
     * @param destinationY 目的地Y坐标
     * @return 移动状态
     */
    public MovementState detectMovementState(int destinationX, int destinationY) throws IOException, InterruptedException {
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "开始检测角色移动状态，目的地: (" + destinationX + "," + destinationY + ")");

        // 检查是否在战斗中
        if (gameStateDetector.isInBattle()) {
            TaskStepNotifier.notifyStep(deviceId, "角色正在战斗中");
            return MovementState.IN_BATTLE;
        }

        // 第一次获取坐标
        int[] firstCoordinates = commonActions.ocrZuobiao();
        if (firstCoordinates[0] == -1 || firstCoordinates[1] == -1) {
            TaskStepNotifier.notifyStep(deviceId, "无法识别当前坐标");
            return MovementState.UNKNOWN;
        }

        TaskStepNotifier.notifyStep(deviceId, "第一次坐标识别: (" + firstCoordinates[0] + "," + firstCoordinates[1] + ")");

        // 等待2秒
        Thread.sleep(1000);

        // 第二次获取坐标
        int[] secondCoordinates = commonActions.ocrZuobiao();
        if (secondCoordinates[0] == -1 || secondCoordinates[1] == -1) {
            TaskStepNotifier.notifyStep(deviceId, "无法识别第二次坐标");
            return MovementState.UNKNOWN;
        }

        TaskStepNotifier.notifyStep(deviceId, "第二次坐标识别: (" + secondCoordinates[0] + "," + secondCoordinates[1] + ")");

        // 判断移动状态
        return analyzeMovementState(firstCoordinates, secondCoordinates, destinationX, destinationY);
    }

    /**
     * 分析移动状态
     * @param firstCoords 第一次坐标
     * @param secondCoords 第二次坐标
     * @param destX 目的地X坐标
     * @param destY 目的地Y坐标
     * @return 移动状态
     */
    private MovementState analyzeMovementState(int[] firstCoords, int[] secondCoords, int destX, int destY) {
        String deviceId = context.getDeviceId();
        
        // 检查两次坐标是否相同
        boolean coordinatesSame = (firstCoords[0] == secondCoords[0] && firstCoords[1] == secondCoords[1]);
        
        // 检查是否已经到达目的地
        boolean atDestination = (firstCoords[0] == destX && firstCoords[1] == destY);
        
        if (coordinatesSame) {
            if (atDestination) {
                TaskStepNotifier.notifyStep(deviceId, "角色已到达目的地");
                return MovementState.STATIONARY;
            } else {
                TaskStepNotifier.notifyStep(deviceId, "角色原地不动，且未到达目的地，需要重新激活");
                return MovementState.ABNORMAL_MOVEMENT;
            }
        } else {
            // 坐标不同，说明在移动
            return MovementState.MOVING_TO_DESTINATION;
        }
    }

    /**
     * 计算两点间距离
     * @param x1 第一个点的X坐标
     * @param y1 第一个点的Y坐标
     * @param x2 第二个点的X坐标
     * @param y2 第二个点的Y坐标
     * @return 距离
     */
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * 等待角色到达目的地
     * @param destinationX 目的地X坐标
     * @param destinationY 目的地Y坐标
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否成功到达目的地
     */
    public boolean waitForDestination(int destinationX, int destinationY, int timeoutSeconds) throws IOException, InterruptedException {
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "等待角色到达目的地: (" + destinationX + "," + destinationY + ")，超时时间: " + timeoutSeconds + "秒");

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while ((System.currentTimeMillis() - startTime) < timeoutMs) {
            // 检查任务状态
            if (Thread.currentThread().isInterrupted()) {
                TaskStepNotifier.notifyStep(deviceId, "任务被中断，停止等待");
                return false;
            }

            // 获取当前坐标
            int[] currentCoords = commonActions.ocrZuobiao();
            if (currentCoords[0] == -1 || currentCoords[1] == -1) {
                TaskStepNotifier.notifyStep(deviceId, "无法识别当前坐标，继续等待");
                Thread.sleep(1000);
                continue;
            }

            // 检查是否到达目的地
            if (currentCoords[0] == destinationX && currentCoords[1] == destinationY) {
                TaskStepNotifier.notifyStep(deviceId, "角色已到达目的地: (" + destinationX + "," + destinationY + ")");
                return true;
            }

            // 检查是否在战斗中
            if (gameStateDetector.isInBattle()) {
                TaskStepNotifier.notifyStep(deviceId, "角色正在战斗中，等待战斗结束");
                while (gameStateDetector.isInBattle()) {
                    Thread.sleep(1000);
                    if ((System.currentTimeMillis() - startTime) >= timeoutMs) {
                        TaskStepNotifier.notifyStep(deviceId, "等待超时");
                        return false;
                    }
                }
            }

            TaskStepNotifier.notifyStep(deviceId, "当前坐标: (" + currentCoords[0] + "," + currentCoords[1] + ")，继续等待");
            Thread.sleep(2000);
        }

        TaskStepNotifier.notifyStep(deviceId, "等待超时，未能到达目的地");
        return false;
    }

    /**
     * 检测并处理移动异常
     * @param destinationX 目的地X坐标
     * @param destinationY 目的地Y坐标
     * @param maxRetries 最大重试次数
     * @return 是否成功处理
     */
    public boolean handleAbnormalMovement(int destinationX, int destinationY, int maxRetries) throws IOException, InterruptedException {
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "开始处理移动异常，最大重试次数: " + maxRetries);

        for (int retry = 0; retry < maxRetries; retry++) {
            TaskStepNotifier.notifyStep(deviceId, "第" + (retry + 1) + "次尝试处理移动异常");

            // 检测当前移动状态
            MovementState state = detectMovementState(destinationX, destinationY);
            
            switch (state) {
                case MOVING_TO_DESTINATION:
                    TaskStepNotifier.notifyStep(deviceId, "角色正在正常移动，无需处理");
                    return true;
                    
                case STATIONARY:
                    if (waitForDestination(destinationX, destinationY, 10)) {
                        TaskStepNotifier.notifyStep(deviceId, "角色已到达目的地");
                        return true;
                    }
                    break;
                    
                case ABNORMAL_MOVEMENT:
                    TaskStepNotifier.notifyStep(deviceId, "检测到移动异常，尝试重新激活");
                    // 这里可以添加重新激活的逻辑，比如重新点击目的地
                    commonActions.clickInputPos(destinationX + "," + destinationY);
                    break;
                    
                case IN_BATTLE:
                    TaskStepNotifier.notifyStep(deviceId, "角色正在战斗中，等待战斗结束");
                    while (gameStateDetector.isInBattle()) {
                        Thread.sleep(1000);
                    }
                    break;
                    
                case UNKNOWN:
                    TaskStepNotifier.notifyStep(deviceId, "无法识别移动状态，重试");
                    break;
            }

            // 等待一段时间后重试
            Thread.sleep(3000);
        }

        TaskStepNotifier.notifyStep(deviceId, "处理移动异常失败，已达到最大重试次数");
        return false;
    }
} 