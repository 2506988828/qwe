package com.my.qwe.controller;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.util.JsonUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class HumanLikeController {
    private static final String CONFIG_PATH = "D:/myapp/config/human_config.json";
    private static Map<String, Object> config;


    static {
        try {
            config = JsonUtil.readJsonFileToMap(new File(CONFIG_PATH));
            System.out.println("[HumanLikeController] 加载配置成功：" + config);
        } catch (Exception e) {
            throw new RuntimeException("加载人性化配置失败，请检查配置文件：" + CONFIG_PATH, e);
        }
    }

    private Point applyRandomOffset(int x, int y) {
        int offsetX = ((Number) config.getOrDefault("clickOffsetX", 0)).intValue();
        int offsetY = ((Number) config.getOrDefault("clickOffsetY", 0)).intValue();

        int actualX = x + (int) (Math.random() * offsetX * 2) - offsetX;
        int actualY = y + (int) (Math.random() * offsetY * 2) - offsetY;

        return new Point(actualX, actualY);
    }

    // 模拟点击操作
    public void click(String deviceId, int x, int y) throws IOException {
        Point actual = applyRandomOffset(x, y);

        // 执行点击操作，实际调用设备相关的API,
        DeviceHttpClient.click(deviceId,"left", actual.x, actual.y,0);
    }

    //在全屏寻找图片，并在偏移量内进行点击

    public void clickImg(String deviceId,String filename){

        try {
            String imgPath = "D:\\myapp\\images\\"+filename+".bmp";
            int []pos = DeviceHttpClient.findImage(deviceId,imgPath,0.8);
            click(deviceId,pos[0],pos[1] );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /*
    * 根据图片在整个屏幕中截图后查找所有坐标位置
    * */







}
