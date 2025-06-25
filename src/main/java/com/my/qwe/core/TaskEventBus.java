package com.my.qwe.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskEventBus {

    public interface Listener {
        void onStepEvent(TaskStepEvent event);
    }

    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void register(Listener listener) {
        listeners.add(listener);
    }

    public static void unregister(Listener listener) {
        listeners.remove(listener);
    }

    public static void publish(TaskStepEvent event) {
        for (Listener listener : listeners) {
            listener.onStepEvent(event);
        }
    }
}
