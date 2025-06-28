package com.my.qwe.core;

import java.util.ArrayList;
import java.util.List;

public class TaskEventBus {

    public interface Listener {
        void onStepEvent(TaskStepEvent event);
    }

    private static final List<Listener> listeners = new ArrayList<>();

    public static void register(Listener listener) {
        listeners.add(listener);
    }

    public static void post(TaskStepEvent event) {
        for (Listener listener : listeners) {
            listener.onStepEvent(event);
        }
    }
}
