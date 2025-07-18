package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.task.config.IniConfigLoader;

import java.util.Map;

public class WatuTask  implements  ITask {


    @Override
    public void start(TaskContext context, TaskThread thread) {
        HumanLikeController human = new HumanLikeController(thread);


        Map<String,String> config = IniConfigLoader.loadTaskConfig(context.getDeviceName(),"挖图");
        CommonActions common = new CommonActions(context,thread);
        String fentudidian = config.get("分图地点");//读取配置文件中的分图地点
        common.clickAllMatchedGrids(context.getDeviceId(),common.findAllItemIndices(context.getDeviceId(),"藏宝图",0.8));
    }

    @Override
    public String getName() {
        return "挖图";
    }
}
