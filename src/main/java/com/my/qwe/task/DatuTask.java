package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.TaskStepNotifier;

import java.io.IOException;

public class DatuTask implements ITask {

    @Override
    public void start(TaskContext context, TaskThread thread) {
        DeviceHttpClient deviceHttpClient = new DeviceHttpClient();
        TaskStepNotifier.notifyStep(context.getDeviceId(), "开始执行宝图任务");

        Luxian luxian = new Luxian(context,thread);
        try {
            luxian.toScene("麒麟山");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }




    }

    @Override
    public String getName() {
        return "打图";
    }
}
