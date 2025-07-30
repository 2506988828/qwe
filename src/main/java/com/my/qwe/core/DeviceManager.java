package com.my.qwe.core;

import com.my.qwe.task.*;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {

    private static final Map<String, DeviceTask> deviceTaskMap = new ConcurrentHashMap<>();

    public static void startTask(String deviceId, ITask task, TaskContext context) {
        System.out.println("[DeviceManager] Starting task for device: " + deviceId);

        DeviceTask existing = deviceTaskMap.get(deviceId);

        // 已存在任务，判断线程状态
        if (existing != null) {
            TaskThread existingThread = existing.getThread();
            TaskState state = existingThread.getTaskState();

            System.out.println("[DeviceManager] Existing task found, state: " + state +
                    ", thread alive: " + existingThread.isAlive());

            if (state == TaskState.RUNNING) {
                JOptionPane.showMessageDialog(null, "任务正在运行，不能重复开始");
                return;
            }
            if (state == TaskState.PAUSED) {
                JOptionPane.showMessageDialog(null, "任务已暂停，请点击继续或停止后再开始");
                return;
            }

            // 如果旧线程仍在运行，确保先停止它
            if (existingThread.isAlive()) {
                System.out.println("[DeviceManager] Stopping old thread before starting new one");
                existingThread.stopTask();
                try {
                    existingThread.join(2000); // 等待最多2秒
                } catch (InterruptedException e) {
                    System.out.println("[DeviceManager] Interrupted while waiting for old thread to stop");
                }
            }
        }

        // 新建线程
        System.out.println("[DeviceManager] Creating new TaskThread for device: " + deviceId);
        TaskThread thread = new TaskThread(deviceId, task, context);
        DeviceTask deviceTask = new DeviceTask(deviceId, task, context, thread);
        deviceTask.setTaskName(task.getClass().getSimpleName());

        deviceTaskMap.put(deviceId, deviceTask);
        thread.start();

        System.out.println("[DeviceManager] New thread started for device: " + deviceId);
    }

    public static void pauseTask(String deviceId) {
        System.out.println("[DeviceManager] Pausing task for device: " + deviceId);
        DeviceTask task = deviceTaskMap.get(deviceId);
        if (task != null) {
            task.getThread().pauseTask();
            task.setThreadStatus("已暂停");
        }
    }

    public static void resumeTask(String deviceId) {
        System.out.println("[DeviceManager] Resuming task for device: " + deviceId);
        DeviceTask task = deviceTaskMap.get(deviceId);
        if (task != null) {
            task.getThread().resumeTask();
            task.setThreadStatus("运行中");
        }
    }

    public static void stopTask(String deviceId) {
        System.out.println("[DeviceManager] Stopping task for device: " + deviceId);
        DeviceTask task = deviceTaskMap.get(deviceId);
        if (task != null) {
            task.getThread().stopTask();
            task.setThreadStatus("已停止");
        }
    }

    public static DeviceTask getDeviceTask(String deviceId) {
        return deviceTaskMap.get(deviceId);
    }
}