package com.my.qwe.task;

import com.my.qwe.core.TaskEventBus;
import com.my.qwe.core.TaskStepEvent;

public class BaotuTask implements ITask {

    private volatile boolean stopped = false;
    private volatile boolean paused = false;



    @Override
    public void execute(TaskContext context) throws Exception {
        String deviceId = context.getDeviceId();

        for (int i = 1; i <= 5; i++) {
            if (stopped) break;

            while (paused) {
                Thread.sleep(200);
            }

            String stepDesc = "执行宝图步骤 " + i;
            System.out.println("[" + deviceId + "] " + stepDesc);

            // 🔔 发布事件
            TaskEventBus.publish(new TaskStepEvent(deviceId, stepDesc, i));

            Thread.sleep(1000);
        }

        TaskEventBus.publish(new TaskStepEvent(deviceId, "宝图任务完成", 999));
    }

    @Override
    public void stop() {
        stopped = true;
        System.out.println("任务被停止");
    }

    @Override
    public void pause() {
        paused = true;
        System.out.println("任务已暂停");
    }

    @Override
    public void resume() {
        paused = false;
        System.out.println("任务已恢复");
    }
}
