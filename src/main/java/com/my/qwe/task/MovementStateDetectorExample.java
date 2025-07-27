package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;

import java.io.IOException;

/**
 * MovementStateDetector 使用示例
 * 展示如何在任务中使用移动状态检测功能
 */
public class MovementStateDetectorExample {
    
    /**
     * 示例：在任务中使用移动状态检测
     */
    public static void exampleUsage(TaskContext context, TaskThread thread) throws IOException, InterruptedException {
        // 初始化相关组件
        DeviceHttpClient httpClient = new DeviceHttpClient();
        GameStateDetector gameStateDetector = new GameStateDetector(context, httpClient);
        CommonActions commonActions = new CommonActions(context, thread);
        MovementStateDetector movementDetector = new MovementStateDetector(context, commonActions, gameStateDetector);
        
        // 定义目的地坐标
        int destinationX = 192;
        int destinationY = 133;
        
        TaskStepNotifier.notifyStep(context.getDeviceId(), "开始移动状态检测示例");
        
        // 示例1：检测当前移动状态
        MovementStateDetector.MovementState state = movementDetector.detectMovementState(destinationX, destinationY);
        
        switch (state) {
            case MOVING_TO_DESTINATION:
                TaskStepNotifier.notifyStep(context.getDeviceId(), "角色正在前往目的地");
                break;
            case STATIONARY:
                TaskStepNotifier.notifyStep(context.getDeviceId(), "角色原地不动");
                break;
            case ABNORMAL_MOVEMENT:
                TaskStepNotifier.notifyStep(context.getDeviceId(), "检测到移动异常，需要重新激活");
                break;
            case IN_BATTLE:
                TaskStepNotifier.notifyStep(context.getDeviceId(), "角色正在战斗中");
                break;
            case UNKNOWN:
                TaskStepNotifier.notifyStep(context.getDeviceId(), "无法识别移动状态");
                break;
        }
        
        // 示例2：等待角色到达目的地
        boolean reached = movementDetector.waitForDestination(destinationX, destinationY, 30);
        if (reached) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "角色已到达目的地");
        } else {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "角色未能到达目的地");
        }
        
        // 示例3：处理移动异常
        boolean handled = movementDetector.handleAbnormalMovement(destinationX, destinationY, 5);
        if (handled) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "移动异常处理成功");
        } else {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "移动异常处理失败");
        }
    }
    
    /**
     * 示例：在循环中持续监控移动状态
     */
    public static void continuousMonitoringExample(TaskContext context, TaskThread thread, int destinationX, int destinationY) throws IOException, InterruptedException {
        DeviceHttpClient httpClient = new DeviceHttpClient();
        GameStateDetector gameStateDetector = new GameStateDetector(context, httpClient);
        CommonActions commonActions = new CommonActions(context, thread);
        MovementStateDetector movementDetector = new MovementStateDetector(context, commonActions, gameStateDetector);
        
        int maxAttempts = 10;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            attempt++;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "第" + attempt + "次检测移动状态");
            
            // 检测移动状态
            MovementStateDetector.MovementState state = movementDetector.detectMovementState(destinationX, destinationY);
            
            switch (state) {
                case MOVING_TO_DESTINATION:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "角色正在移动，继续监控");
                    Thread.sleep(5000); // 等待5秒后再次检测
                    break;
                    
                case STATIONARY:
                    // 检查是否已到达目的地
                    int[] currentCoords = commonActions.ocrZuobiao();
                    if (currentCoords[0] == destinationX && currentCoords[1] == destinationY) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "角色已到达目的地");
                        return; // 成功到达，退出循环
                    } else {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "角色原地不动，尝试重新激活");
                        // 这里可以添加重新激活的逻辑
                        Thread.sleep(2000);
                    }
                    break;
                    
                case ABNORMAL_MOVEMENT:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "检测到移动异常，尝试处理");
                    boolean handled = movementDetector.handleAbnormalMovement(destinationX, destinationY, 3);
                    if (!handled) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "处理移动异常失败");
                        return; // 处理失败，退出循环
                    }
                    break;
                    
                case IN_BATTLE:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "角色正在战斗中，等待战斗结束");
                    while (gameStateDetector.isInBattle()) {
                        Thread.sleep(1000);
                    }
                    break;
                    
                case UNKNOWN:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "无法识别移动状态，重试");
                    Thread.sleep(2000);
                    break;
            }
            
            // 检查任务是否被中断
            if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "任务被中断");
                return;
            }
        }
        
        TaskStepNotifier.notifyStep(context.getDeviceId(), "达到最大尝试次数，移动监控结束");
    }
} 