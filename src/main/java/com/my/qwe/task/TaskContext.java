package com.my.qwe.task;

import com.my.qwe.task.config.TaskConfigLoader;

import java.util.Map;

public class TaskContext {

    private final String deviceId;
    private final Map<String, Object> config;
    private boolean debugMode = false;

    // 私有构造器，外部不直接调用
    public TaskContext(String deviceId, Map<String, Object> config) {
        this.deviceId = deviceId;
        this.config = config;
    }

    // 静态工厂方法：从配置Map构造
    public static TaskContext fromConfigMap(String deviceId, Map<String, Object> config) {
        return new TaskContext(deviceId, config);
    }

    // 静态工厂方法：从任务类型加载配置构造
    public static TaskContext fromTaskType(String deviceId, String taskType) {
        Map<String, Object> config = TaskConfigLoader.loadConfig(deviceId, taskType);
        return new TaskContext(deviceId, config);
    }

    // Getter和Setter

    public String getDeviceId() {
        return deviceId;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public Object get(String key) {
        if (config == null) return null;
        return config.get(key);
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
