package com.my.qwe.task;

import com.my.qwe.task.TaskState;
import com.my.qwe.task.TaskStepNotifier;

public class TaskThread extends Thread {
    private final String deviceId;
    private final ITask task;
    private final TaskContext context;

    private volatile TaskState taskState = TaskState.INIT;
    private static volatile boolean shouldPause = false;
    private static volatile boolean shouldStop = false;
    private static volatile boolean paused = false;  // ✅ 加上这个字段

    public TaskThread(String deviceId, ITask task, TaskContext context) {
        this.deviceId = deviceId;
        this.task = task;
        this.context = context;
        setName("TaskThread-" + deviceId);

        this.shouldStop = false;
        this.paused = false;
        this.taskState = TaskState.NEW;  // ✅ 关键修复
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public void run() {
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
        if (taskState == TaskState.RUNNING) {
            shouldPause = true;
            taskState = TaskState.PAUSED;

        }
    }

    public void resumeTask() {
        if (taskState == TaskState.PAUSED) {
            shouldPause = false;
            taskState = TaskState.RUNNING;
            synchronized (this) {
                notify();
            }

        }
    }

    public void stopTask() {
        shouldStop = true;
        taskState = TaskState.STOPPED;
        interrupt();
        System.out.println("[Thread] Stop called for: " + getName());
    }



    public void checkPause() {
        synchronized (this) {
            while (shouldPause && !shouldStop) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public boolean isStopped() {
        return shouldStop || taskState == TaskState.STOPPED;
    }
}
