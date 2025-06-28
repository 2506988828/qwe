package com.my.qwe.controller;

import com.my.qwe.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class HumanConfig {
    private static final String CONFIG_PATH = "D:/myapp/human_config.json"; // 可换成 ConfigUtil.getHumanConfigFile()
    private static Map<String, Object> config;

    // 懒加载配置
    public static synchronized void loadConfigIfNeeded() {
        if (config != null) return;
        try {
            File file = new File(CONFIG_PATH);
            if (!file.exists()) {

                JsonUtil.writeMapToJsonFile(config, file); // 自动创建默认配置
            } else {
                config = JsonUtil.readJsonFileToMap(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
