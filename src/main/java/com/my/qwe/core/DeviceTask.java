package com.my.qwe.core;

import com.my.qwe.task.ITask;
import com.my.qwe.task.TaskContext;
import com.my.qwe.task.TaskThread;

public class DeviceTask {
    private final String deviceId;
    private final ITask task;
    private final TaskContext context;
    private final TaskThread thread;

    private String currentStep = "初始化";
    private String threadStatus = "未开始";
    private String taskName = "无任务";

    public DeviceTask(String deviceId, ITask task, TaskContext context, TaskThread thread) {
        this.deviceId = deviceId;
        this.task = task;
        this.context = context;
        this.thread = thread;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public ITask getTask() {
        return task;
    }

    public TaskContext getContext() {
        return context;
    }

    public TaskThread getThread() {
        return thread;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getThreadStatus() {
        return threadStatus;
    }

    public void setThreadStatus(String threadStatus) {
        this.threadStatus = threadStatus;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
}
