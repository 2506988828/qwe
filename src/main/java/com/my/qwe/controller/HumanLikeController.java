package com.my.qwe.controller;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.TaskThread;
import org.ini4j.Wini;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HumanLikeController {
    private static final String CONFIG_PATH = "D:/myapp/config/config.ini";
    private final TaskThread taskThread;

    // 配置参数
    private static int clickOffsetX = 0;
    private static int clickOffsetY = 0;

    public HumanLikeController(TaskThread taskThread) {
        this.taskThread = taskThread;
    }



    private Point applyRandomOffset(int x, int y ,int xp , int yp) {
        int actualX = x + (int) (Math.random() * xp * 2) - xp;
        int actualY = y + (int) (Math.random() * yp * 2) - yp;
        return new Point(actualX, actualY);
    }

    // 模拟点击操作
    public void click(String deviceId, int x, int y) throws IOException {

        DeviceHttpClient.click(deviceId, "left", x, y);
    }

    public void sendkey(String deviceId,String key) throws IOException {
        DeviceHttpClient.sendkey(deviceId,key);


    }

    //坐标加减随机数后点击
    public void click(String deviceId, int x, int y, int xp, int yp) throws IOException {
        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return ;
        taskThread.checkPause();
        Point actual = applyRandomOffset(x, y, xp, yp);
        DeviceHttpClient.click(deviceId, "left", actual.x, actual.y);
    }
    public void doubleclick(String deviceId, int x, int y, int xp, int yp) throws IOException {
        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return ;
        taskThread.checkPause();
        Point actual = applyRandomOffset(x, y, xp, yp);
        DeviceHttpClient.doubleclick(deviceId, "left", actual.x, actual.y);
    }





    // 在全屏寻找图片，并点击
    public void clickImg(String deviceId, String filename,int px,int py) {
        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return ;
        taskThread.checkPause();
        try {

            int[] pos = DeviceHttpClient.findImage(deviceId, filename, 0.8);
            click(deviceId, pos[0], pos[1],px,py);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
