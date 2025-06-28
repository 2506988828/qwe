package com.my.qwe.core;

import com.my.qwe.task.ITask;
import com.my.qwe.task.TaskContext;

public class DeviceTask implements Runnable {
    private final String deviceId;
    private final ITask task;
    private final TaskContext context;

    private volatile boolean stopped = false;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    private Thread runningThread;

    public DeviceTask(String deviceId, ITask task, TaskContext context) {
        this.deviceId = deviceId;
        this.task = task;
        this.context = context;
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        try {
            task.init(context);

            while (!stopped) {
                synchronized (pauseLock) {
                    while (paused) {
                        pauseLock.wait();
                    }
                }

                boolean finished = task.executeStep(context);
                if (finished) {
                    break;
                }

                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            context.log("任务执行异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                task.cleanup(context);
            } catch (Exception e) {
                context.log("任务清理异常：" + e.getMessage());
            }
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    public void stop() {
        stopped = true;
        resume();
        if (runningThread != null) {
            runningThread.interrupt();
        }
    }
}
