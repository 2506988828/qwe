package com.my.qwe.controller;

import com.my.qwe.http.DeviceHttpClient;
import org.ini4j.Wini;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HumanLikeController {
    private static final String CONFIG_PATH = "D:/myapp/config/config.ini";

    // 配置参数
    private static int clickOffsetX = 0;
    private static int clickOffsetY = 0;

    /*static {
        try {
            File configFile = new File(CONFIG_PATH);
            if (!configFile.exists()) {
                // 你可以在这里自动创建默认配置文件，或提醒用户
                throw new RuntimeException("配置文件不存在：" + CONFIG_PATH);
            }
            Wini ini = new Wini(configFile);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            // 从默认节读取中文键
            clickOffsetX = ini.get("人性化", "点击偏移X", Integer.class);  // 返回 Integer 类型

            clickOffsetY = ini.get("人性化", "点击偏移Y", Integer.class);  // 返回 Integer 类型


            System.out.printf("human加载配置成功：");
        } catch (Exception e) {
            throw new RuntimeException("加载配置失败，请检查配置文件：" + CONFIG_PATH, e);
        }
    }*/

    private Point applyRandomOffset(int x, int y ,int xp , int yp) {
        int actualX = x + (int) (Math.random() * xp * 2) - xp;
        int actualY = y + (int) (Math.random() * yp * 2) - yp;
        return new Point(actualX, actualY);
    }

    // 模拟点击操作
    public void click(String deviceId, int x, int y) throws IOException {

        DeviceHttpClient.click(deviceId, "left", x, y);
    }
    //坐标加减随机数后点击
    public void click(String deviceId, int x, int y, int xp, int yp) throws IOException {
        Point actual = applyRandomOffset(x, y, xp, yp);
        DeviceHttpClient.click(deviceId, "left", actual.x, actual.y);
    }



    // 在全屏寻找图片，并点击
    public void clickImg(String deviceId, String filename) {
        try {
            String imgPath = "D:\\myapp\\images\\" + filename + ".bmp";
            int[] pos = DeviceHttpClient.findImage(deviceId, imgPath, 0.8);
            click(deviceId, pos[0], pos[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
