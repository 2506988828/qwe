package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.TaskConfigLoader;
import com.my.qwe.util.JsonUtil;

import java.util.Map;

public class FentuTask implements ITask {
    private Map<String, Map<String, String>> config; // 存储从配置文件加载的任务配置


    //执行分图操作

    @Override
    public void execute(TaskContext context) throws Exception {

    }

    @Override
    public void init(TaskContext context) throws Exception {

    }

    public boolean executeStep(TaskContext context) throws Exception {


        String deviceId = context.getDeviceId();
        HumanLikeController human = new HumanLikeController();
        this.config = TaskConfigLoader.loadConfig(context.getName(), "baotu");

        DeviceHttpClient.findImages(deviceId,"数字6","数字6",0.8);


        return true; // 任务完成
    }

    @Override
    public void cleanup(TaskContext context) throws Exception {

    }




}
