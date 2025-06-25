package com.my.qwe.core;

public class TaskStepEvent {
    private final String deviceId;
    private final String stepDescription;
    private final int stepIndex;

    public TaskStepEvent(String deviceId, String stepDescription, int stepIndex) {
        this.deviceId = deviceId;
        this.stepDescription = stepDescription;
        this.stepIndex = stepIndex;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public int getStepIndex() {
        return stepIndex;
    }
}
