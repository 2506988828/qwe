package com.my.qwe.task;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 通知任务步骤进展的工具类
 */
public class TaskStepNotifier {
    private static final Map<String, Consumer<String>> listenerMap = new HashMap<>();

    public static void registerListener(String deviceId, Consumer<String> listener) {
        listenerMap.put(deviceId, listener);
    }

    public static void notifyStep(String deviceId, String step) {
        Consumer<String> listener = listenerMap.get(deviceId);
        if (listener != null) {
            // 先清空（显示一个过渡状态）
            listener.accept("...");

            // 使用 Swing Timer 延迟200ms再显示真正内容
            Timer timer = new Timer(100, e -> listener.accept(step));
            timer.setRepeats(false);  // 只执行一次
            timer.start();
        }
    }

}
