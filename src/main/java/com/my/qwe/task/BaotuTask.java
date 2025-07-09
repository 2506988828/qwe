package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.controller.TaskConfigController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.TaskConfigLoader;
import com.my.qwe.util.ConfigUtil;
import com.my.qwe.util.JsonUtil;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BaotuTask implements ITask {


    Wini wini =new Wini();
    static Wini allini = new Wini();

    private TaskConfigController configController;
    private Map<String, Map<String, String>> taskConfig;



    @Override
    public void execute(TaskContext context) throws Exception {

        // 执行任务的入口
        while (!executeStep(context)) {
            // 如果未完成，继续执行
        }
    }

    @Override
    public void init(TaskContext context) throws Exception {

    }
    static {

        try {
            allini = new  Wini(new File("D:/myapp/config/config.ini"));
            allini.getConfig().setFileEncoding(StandardCharsets.UTF_8);
            System.out.println("全局配置加载成功");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean executeStep(TaskContext context) throws Exception {

        int[] ocrRect = ConfigUtil.parseIntArray(allini.get("全局", "坐标识别地图名称"));

        for (int i = 0; i < ocrRect.length; i++) {
            System.out.println(ocrRect[i]);
        }

        wini = configController.loadConfig(context.getName(), "挖图");
        wini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

        System.out.println(wini);

        System.out.println("进入宝图任务");


        String deviceId = context.getDeviceId();
        HumanLikeController human = new HumanLikeController();



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
