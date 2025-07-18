package com.my.qwe.task;

import com.my.qwe.task.TaskStepNotifier;

public class DatuTask implements ITask {

    @Override
    public void start(TaskContext context, TaskThread thread) {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "开始执行宝图任务");



        try {
            Thread.sleep(2000);
            // 示例步骤 1：初始化
            if (thread.isStopped()) return;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "步骤 1：读取宝图配置");
            thread.checkPause();
            Thread.sleep(2000);
            // 示例步骤 2：打开背包
            if (thread.isStopped()) return;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "步骤 2：打开背包");
            thread.checkPause();
            // 示例步骤 3：执行识图逻辑
            if (thread.isStopped()) return;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "步骤 3：查找宝图位置");
            thread.checkPause();
            Thread.sleep(2000);
            if (thread.isStopped()) return;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "步骤 4444：查找宝图位置");
            thread.checkPause();
            Thread.sleep(2000);
            if (thread.isStopped()) return;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "步骤 555555555：查找宝图位置");
            thread.checkPause();
            Thread.sleep(2000);
            if (thread.isStopped()) return;
            TaskStepNotifier.notifyStep(context.getDeviceId(), "步骤 3666666666666666666666666：查找宝图位置");
            thread.checkPause();
            Thread.sleep(2000);

            // 可继续添加其他逻辑步骤

            TaskStepNotifier.notifyStep(context.getDeviceId(), "宝图任务完成");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }





    }

    @Override
    public String getName() {
        return "打图";
    }
}
