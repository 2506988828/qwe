package com.my.qwe.core;

import com.my.qwe.task.ITask;
import com.my.qwe.task.TaskContext;

public class DeviceTask extends Thread {

    private final String deviceId;
    private final ITask task;
    private final TaskContext context;

    private volatile boolean running = true;
    private volatile boolean paused = false;

    public DeviceTask(String deviceId, ITask task, TaskContext context) {
        this.deviceId = deviceId;
        this.task = task;
        this.context = context;
        setName("DeviceTask-" + deviceId);
    }

    @Override
    public void run() {
        try {
            task.execute(context);
        } catch (Exception e) {
            System.err.println("任务执行出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopTask() {
        running = false;
        task.stop();
        this.interrupt(); // 防止阻塞
    }

    public void pauseTask() {
        paused = true;
        task.pause();
    }

    public void resumeTask() {
        paused = false;
        task.resume();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }
}
