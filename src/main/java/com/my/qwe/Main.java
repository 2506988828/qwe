package com.my.qwe;

import com.my.qwe.init.AppInitializer;
import com.my.qwe.core.TaskEventBus;
import com.my.qwe.core.TaskStepEvent;
import com.my.qwe.ui.MainUI;

public class Main {
    public static void main(String[] args) {
        AppInitializer.initDirectories();

        // 注册任务步骤监听器，用于控制台日志（或 UI 通知）
        TaskEventBus.register(new TaskEventBus.Listener() {
            @Override
            public void onStepEvent(TaskStepEvent event) {
                System.out.println(">> [UI更新] " + event.getDeviceId()
                        + ": 第" + event.getStepIndex() + "步 - " + event.getStepDescription());
            }
        });

        // 启动主界面
        MainUI.launch();
    }

}
