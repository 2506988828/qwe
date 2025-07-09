package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.TaskConfigLoader;
import com.my.qwe.util.JsonUtil;

import java.io.File;
import java.util.Map;

public class BaotuTask implements ITask {

    private int step = 0; // 记录任务的步骤
    private Map<String, Map<String, String>> config; // 存储从配置文件加载的任务配置


    @Override
    public void execute(TaskContext context) throws Exception {
        // 从配置文件加载任务参数

        this.config = TaskConfigLoader.loadConfig(context.getName(), "baotu");

        // 执行任务的入口
        while (!executeStep(context)) {
            // 如果未完成，继续执行
        }
        context.log("宝图任务执行完毕");
    }

    @Override
    public void init(TaskContext context) throws Exception {

    }

    @Override
    public boolean executeStep(TaskContext context) throws Exception {


        System.out.println("进入宝图任务");


        String deviceId = context.getDeviceId();
        HumanLikeController human = new HumanLikeController();
        this.config = TaskConfigLoader.loadConfig(context.getName(), "baotu");

        System.out.println("识别当前位置");
        // 获取OCR识别区域（假设左上角有“宝象国”文字）
        int[] ocrRect = JsonUtil.parseRect(config.get("ocrRect"));

        String locationText = DeviceHttpClient.ocr(deviceId, ocrRect);
        context.log("当前位置：" + locationText);
        // 判断是否在宝象国
        if (locationText.contains("宝象国")) {

            context.log("点击仓库管理员，打开仓库");

            human.click(deviceId,286,127);
            human.click(deviceId,617,246);
        } else {
            context.log("不在宝象国，打开背包...");
            human.click(deviceId,658, 375);   //打开道具
            Thread.sleep(1000); // 等待背包加载


            context.log("使用飞行符，前往宝象国");
            human.clickImg(deviceId,"飞行符");
            Thread.sleep(1000);
            human.clickImg(deviceId,"使用");
            Thread.sleep(1000);
            human.clickImg(deviceId,"宝象国");
            Thread.sleep(1000);

            context.log("点击仓库管理员,打开仓库");

            human.click(deviceId,286,127);
            human.click(deviceId,617,246);

        }

        // 假设任务执行最多执行 1 步（因为本步骤较为复杂，实际情况下可拆分成多个步骤）
        return true; // 任务完成
    }


    @Override
    public void cleanup(TaskContext context) throws Exception {

    }
}
