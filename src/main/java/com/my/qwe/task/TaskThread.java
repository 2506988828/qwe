package com.my.qwe.task;

import com.my.qwe.task.TaskState;
import com.my.qwe.task.TaskStepNotifier;

public class TaskThread extends Thread {
    private final String deviceId;
    private final ITask task;
    private final TaskContext context;

    private volatile TaskState taskState = TaskState.INIT;

    // 关键修复：将静态字段改为实例字段，避免线程间状态污染
    private volatile boolean shouldPause = false;
    private volatile boolean shouldStop = false;
    private volatile boolean paused = false;

    public TaskThread(String deviceId, ITask task, TaskContext context) {
        this.deviceId = deviceId;
        this.task = task;
        this.context = context;
        setName("TaskThread-" + deviceId);

        // 确保每个新实例都有干净的初始状态
        this.shouldStop = false;
        this.shouldPause = false;
        this.paused = false;
        this.taskState = TaskState.NEW;

        System.out.println("[Thread] Created new TaskThread: " + getName() +
                " with clean state (shouldStop=" + shouldStop +
                ", shouldPause=" + shouldPause + ")");
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public void run() {
        System.out.println("[Thread] Starting run() for: " + getName() +
                " (shouldStop=" + shouldStop + ", shouldPause=" + shouldPause + ")");

        taskState = TaskState.RUNNING;
        try {
            task.start(context, this);
        } catch (Exception e) {
            e.printStackTrace();
            TaskStepNotifier.notifyStep(deviceId, "任务异常中止：" + e.getMessage());
        } finally {
            taskState = TaskState.STOPPED;
            System.out.println("[Thread] Run finished for: " + getName());
        }
    }

    public void pauseTask() {
        System.out.println("[Thread] Pause requested for: " + getName());
        if (taskState == TaskState.RUNNING) {
            shouldPause = true;
            taskState = TaskState.PAUSED;
        }
    }

    public void resumeTask() {
        System.out.println("[Thread] Resume requested for: " + getName());
        if (taskState == TaskState.PAUSED) {
            shouldPause = false;
            taskState = TaskState.RUNNING;
            synchronized (this) {
                notify();
            }
        }
    }

    public void stopTask() {
        System.out.println("[Thread] Stop requested for: " + getName());
        shouldStop = true;
        taskState = TaskState.STOPPED;
        interrupt();
    }

    public void checkPause() {
        synchronized (this) {
            while (shouldPause && !shouldStop) {
                try {
                    System.out.println("[Thread] " + getName() + " is paused, waiting...");
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("[Thread] " + getName() + " interrupted while paused");
                    break;
                }
            }
        }
    }

    public boolean isStopped() {
        return shouldStop || taskState == TaskState.STOPPED;
    }

    // 添加一个静态的 sleep 方法，避免与 Thread.sleep 冲突
    public static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}