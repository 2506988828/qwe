package com.my.qwe.manager;


import com.my.qwe.task.TaskContext;
import com.my.qwe.task.config.IniConfigLoader;

public class ConfigurationManager {
    private final IniConfigLoader configLoader;
    private static final String CONFIG_PATH = "config/watu.ini";
    private static final String SECTION_WATU = "挖图";

    public ConfigurationManager(TaskContext context) {
        this.configLoader = new IniConfigLoader(CONFIG_PATH);
        this.configLoader.loadIniFile();
    }

    // 获取藏宝图筛选条件
    public String getMapFilter() {
        return configLoader.getValue(SECTION_WATU, "藏宝图筛选条件", "");
    }

    // 获取已挖图数量
    public int getDugCount() {
        return configLoader.getIntValue(SECTION_WATU, "已挖图数", 0);
    }

    // 增加已挖图数量
    public void incrementDugCount() {
        int current = getDugCount();
        configLoader.setValue(SECTION_WATU, "已挖图数", String.valueOf(current + 1));
        configLoader.save();
    }

    // 获取背包中的藏宝图信息
    public String getBagMaps() {
        return configLoader.getValue(SECTION_WATU, "背包", "");
    }

    // 更新背包中的藏宝图信息
    public void setBagMaps(String mapInfo) {
        configLoader.setValue(SECTION_WATU, "背包", mapInfo);
        configLoader.save();
    }

    // 获取地图坐标信息（根据仓库位置）
    public String getMapScene(int warehouseLocation) {
        return configLoader.getValue("地图坐标", "位置" + warehouseLocation + "_场景", "");
    }

    public int getMapX(int warehouseLocation) {
        return configLoader.getIntValue("地图坐标", "位置" + warehouseLocation + "_X", 0);
    }

    public int getMapY(int warehouseLocation) {
        return configLoader.getIntValue("地图坐标", "位置" + warehouseLocation + "_Y", 0);
    }

    public String getMapType(int warehouseLocation) {
        return configLoader.getValue("地图坐标", "位置" + warehouseLocation + "_类型", "普通");
    }
}