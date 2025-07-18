package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.task.config.IniConfigLoader;

import java.util.Map;

public class ZhuantuTask implements ITask  {


    @Override
    public void start(TaskContext context, TaskThread thread) {
        HumanLikeController human = new HumanLikeController(thread);


        Map<String,String> config = IniConfigLoader.loadTaskConfig(context.getDeviceName(),"分图");
        CommonActions common = new CommonActions(context,thread);
        String fentudidian = config.get("分图地点");
        String ocrdiqu = common.ocrShibieDiqu();
        common.transferTreasureMaps("藏宝图","78806278");

    }

    @Override
    public String getName() {
        return "转图";
    }



}
