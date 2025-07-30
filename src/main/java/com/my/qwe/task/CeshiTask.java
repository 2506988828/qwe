package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;

public class CeshiTask implements ITask{
    @Override
    public void start(TaskContext context, TaskThread thread) throws Exception {
        CommonActions commonActions = new CommonActions(context,thread);
        GameStateDetector gameStateDetector = new GameStateDetector(context,new DeviceHttpClient());
        //打开仓库以后存物资
        if (gameStateDetector.isChuxiansixiaoren()){
            //截图，裁剪，
            String screenshotbase64= DeviceHttpClient.getScreenshotBase64(context.getDeviceId());
            String imgbase64=commonActions.cropImage(screenshotbase64,203,56,350,140);
            commonActions.sixiaoren(imgbase64);
        }

        if (gameStateDetector.isChuxianchengyu()){
            commonActions.sizichengyu();
        }
    }

    @Override
    public String getName() {
        return "";
    }
}
