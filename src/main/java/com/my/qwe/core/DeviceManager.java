package com.my.qwe.core;

import com.my.qwe.task.ITask;
import com.my.qwe.task.TaskContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {

    private static final Map<String, DeviceTask> taskMap = new ConcurrentHashMap<>();

    public static void startTask(String deviceId, ITask task, TaskContext context) {
        if (taskMap.containsKey(deviceId)) {
            System.out.println("设备 " + deviceId + " 的任务已在运行中");
            return;
        }
        DeviceTask deviceTask = new DeviceTask(deviceId, task, context);
        taskMap.put(deviceId, deviceTask);
        deviceTask.start();
    }

    public static void stopTask(String deviceId) {
        DeviceTask task = taskMap.remove(deviceId);
        if (task != null) {
            task.stopTask();
        }
    }

    public static void pauseTask(String deviceId) {
        DeviceTask task = taskMap.get(deviceId);
        if (task != null) {
            task.pauseTask();
        }
    }

    public static void resumeTask(String deviceId) {
        DeviceTask task = taskMap.get(deviceId);
        if (task != null) {
            task.resumeTask();
        }
    }

    public static boolean isRunning(String deviceId) {
        return taskMap.containsKey(deviceId);
    }

    public static Map<String, DeviceTask> getAllTasks() {
        return taskMap;
    }
}
