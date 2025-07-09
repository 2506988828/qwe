package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.controller.TaskConfigController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.TaskConfigLoader;
import com.my.qwe.util.ConfigUtil;
import com.my.qwe.util.JsonUtil;
import org.ini4j.Wini;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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

        wini = configController.loadConfig(context.getName(), "挖图");
        wini.getConfig().setFileEncoding(StandardCharsets.UTF_8);


        System.out.println("进入宝图任务");


        String deviceId = context.getDeviceId();
        HumanLikeController human = new HumanLikeController();



        //String locationText = DeviceHttpClient.ocr(deviceId, ocrRect);
        //context.log("当前位置：" + locationText);
        // 判断是否在宝象国
        /*if (locationText.contains("宝象国")) {
                context.log("点击仓库管理员，打开仓库");


        } else {
            human.click(deviceId,260,500,10,10);
"625358","8|3|df1d0b",0.8
        }*/
        while (true) {
            int[] jiance = DeviceHttpClient.findMultiColor(deviceId, 692, 367, 711, 389, "1f343d", "15|7|627978,15|14|324d54,10|3|c9e3e7,8|9|b5d3df,16|13|8facb5,5|2|34494b,1|4|21363d,5|18|2c4655,6|16|637f8d,6|13|91b5c1,18|15|294548", 0.8, 0);
            int[] jiance2 = DeviceHttpClient.findMultiColor(deviceId, 692, 367, 711, 389, "213741", "12|3|5a7373,6|3|5e818b,8|8|90b7c5,2|14|647f8a,10|2|546b69,0|16|647e89,2|12|243840,4|2|75949b,6|19|647d89,3|1|43595e,6|15|a9d1d8", 0.8, 0);
            if (jiance[0] < 0 && jiance2[0] < 0) {
                context.log("未检测到主画面，请打开界面，关闭所有窗口");
            } else if (jiance[0] > 0 && jiance2[0] < 0) {
                human.click(deviceId, 700, 380, 20, 20);
            }
            Thread.sleep(500);
            if (jiance2[0]>0){
                break;
            }
        }




        return true; // 任务完成
    }


    @Override
    public void cleanup(TaskContext context) throws Exception {

    }
}
