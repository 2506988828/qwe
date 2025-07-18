package com.my.qwe.core;

import com.my.qwe.task.*;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {

    private static final Map<String, DeviceTask> deviceTaskMap = new ConcurrentHashMap<>();

    public static void startTask(String deviceId, ITask task, TaskContext context) {
        DeviceTask existing = deviceTaskMap.get(deviceId);

        // 已存在任务，判断线程状态
        if (existing != null) {
            TaskThread existingThread = existing.getThread();
            TaskState state = existingThread.getTaskState();

            if (state == TaskState.RUNNING) {
                JOptionPane.showMessageDialog(null, "任务正在运行，不能重复开始");
                return;
            }
            if (state == TaskState.PAUSED) {
                JOptionPane.showMessageDialog(null, "任务已暂停，请点击“继续”或“停止”后再开始");
                return;
            }
        }

        // 新建线程
        TaskThread thread = new TaskThread(deviceId, task, context);
        DeviceTask deviceTask = new DeviceTask(deviceId, task, context, thread);
        deviceTask.setTaskName(task.getClass().getSimpleName());

        deviceTaskMap.put(deviceId, deviceTask);
        thread.start();
    }

    public static void pauseTask(String deviceId) {
        DeviceTask task = deviceTaskMap.get(deviceId);
        if (task != null) {
            task.getThread().pauseTask();
            task.setThreadStatus("已暂停");
        }
    }

    public static void resumeTask(String deviceId) {
        DeviceTask task = deviceTaskMap.get(deviceId);
        if (task != null) {
            task.getThread().resumeTask();
            task.setThreadStatus("运行中");
        }
    }

    public static void stopTask(String deviceId) {
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
