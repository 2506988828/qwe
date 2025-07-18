package com.my.qwe.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TaskThreadStatusNotifier {
    private static final Map<String, Consumer<String>> listeners = new ConcurrentHashMap<>();

    public static void register(String deviceId, Consumer<String> listener) {
        listeners.put(deviceId, listener);
    }

    public static void notify(String deviceId, String status) {
        Consumer<String> listener = listeners.get(deviceId);
        if (listener != null) {
            listener.accept(status);
        }
    }

    public static void unregister(String deviceId) {
        listeners.remove(deviceId);
    }
}
