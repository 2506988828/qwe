package com.my.qwe.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskContext {
    private final String deviceId;
    private final String name;
    private final Map<String, Object> params = new ConcurrentHashMap<>();

    public TaskContext(String deviceId, Map<String, Object> initialParams, String name) {
        this.deviceId = deviceId;
        this.name = name;
        if (initialParams != null) {
            this.params.putAll(initialParams);
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName(){
        return name;
    }

    public Object getParam(String key) {
        return params.get(key);
    }

    public void setParam(String key, Object value) {
        params.put(key, value);
    }

    public void log(String message) {

        TaskStepNotifier.notifyStep(deviceId, message);
    }
}
