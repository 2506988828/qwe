package com.my.qwe.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TaskStepNotifier {

    // 每个 deviceId 对应一个监听器（日志订阅者）
    private static final Map<String, Consumer<String>> listeners = new ConcurrentHashMap<>();

    // 注册监听器
    public static void registerListener(String deviceId, Consumer<String> listener) {
        listeners.put(deviceId, listener);
    }

    // 取消监听器
    public static void unregisterListener(String deviceId) {
        listeners.remove(deviceId);
    }

    // 发布任务步骤信息（任务线程调用）
    public static void notifyStep(String deviceId, String message) {
        Consumer<String> listener = listeners.get(deviceId);
        if (listener != null) {
            listener.accept(message);
        }
    }
}
