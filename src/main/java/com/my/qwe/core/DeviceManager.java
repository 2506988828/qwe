package com.my.qwe.core;

import com.my.qwe.task.ITask;
import com.my.qwe.task.TaskContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {

    private static final Map<String, Thread> threadMap = new ConcurrentHashMap<>();
    private static final Map<String, DeviceTask> taskMap = new ConcurrentHashMap<>();

    public static void startTask(String deviceId, ITask task, TaskContext context) {
        if (isRunning(deviceId)) {
            System.out.println("设备 " + deviceId + " 任务已在运行，不能重复启动");
            return; // 任务已经在运行，拒绝启动
        }
        // 正常启动
        DeviceTask deviceTask = new DeviceTask(deviceId, task, context);
        Thread thread = new Thread(deviceTask, "DeviceTask-" + deviceId);
        threadMap.put(deviceId, thread);
        taskMap.put(deviceId, deviceTask);

        thread.start();
    }


    public static void pauseTask(String deviceId) {
        DeviceTask task = taskMap.get(deviceId);
        if (task != null) {
            task.pause();
        }
    }

    public static void resumeTask(String deviceId) {
        DeviceTask task = taskMap.get(deviceId);
        if (task != null) {
            task.resume();
        }
    }

    public static void stopTask(String deviceId) {
        DeviceTask task = taskMap.remove(deviceId);
        Thread thread = threadMap.remove(deviceId);

        if (task != null) {
            task.stop();
        }
        if (thread != null) {
            try {
                thread.join(2000);
            } catch (InterruptedException ignored) {
            }
        }
    }
    public static boolean isRunning(String deviceId) {
        Thread thread = threadMap.get(deviceId);
        return thread != null && thread.isAlive();
    }


}
