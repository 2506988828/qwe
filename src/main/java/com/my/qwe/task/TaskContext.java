package com.my.qwe.task;

import java.util.Map;

/**
 * 任务上下文，包含设备ID、任务配置等
 */
public class TaskContext {
    private final String deviceId;
    private final String deviceName;


    public TaskContext(String deviceId,String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }
}
