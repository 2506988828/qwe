package com.my.qwe;

import com.my.qwe.core.DeviceManager;
import com.my.qwe.task.BaotuTask;
import com.my.qwe.task.TaskContext;
import com.my.qwe.core.TaskEventBus;
import com.my.qwe.core.TaskStepEvent;


import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        // 初始化目录（确保目录存在）
        com.my.qwe.init.AppInitializer.initDirectories();

        // 注册事件监听打印日志
        TaskEventBus.register(event -> System.out.println(">> [任务进度] " + event.getDeviceId() + ": " +
                event.getStepDescription() + " (步骤 " + event.getStepIndex() + ")"));

        String deviceId = "DEVICE-001";
        BaotuTask task = new BaotuTask();

        // 使用任务类型加载配置（配置文件不存在时自动创建默认）
        TaskContext context = TaskContext.fromTaskType(deviceId, "baotu");

        // 启动任务
        DeviceManager.startTask(deviceId, task, context);

        try {
            Thread.sleep(5000);  // 任务执行一会儿
            DeviceManager.pauseTask(deviceId);
            System.out.println("任务已暂停");

            Thread.sleep(3000);
            DeviceManager.resumeTask(deviceId);
            System.out.println("任务已恢复");

            Thread.sleep(5000);
            DeviceManager.stopTask(deviceId);
            System.out.println("任务已停止");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
