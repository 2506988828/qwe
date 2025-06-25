package com.my.qwe.task.config;

import com.my.qwe.util.ConfigUtil;
import com.my.qwe.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TaskConfigLoader {

    /**
     * 读取指定设备和任务类型的配置文件。
     * 如果配置文件不存在，会自动创建一个默认配置文件并返回默认配置。
     */
    public static Map<String, Object> loadConfig(String deviceId, String taskType) {
        try {
            String configDir = ConfigUtil.getConfigDir();
            File configFile = new File(configDir, deviceId + "_" + taskType + ".json");

            if (!configFile.exists()) {
                System.out.println("配置文件不存在，自动创建默认配置文件：" + configFile.getAbsolutePath());
                Map<String, Object> defaultConfig = createDefaultConfig(taskType);
                // 确保目录存在
                File parentDir = configFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                // 写默认配置文件
                JsonUtil.writeMapToJsonFile(defaultConfig, configFile);
                return defaultConfig;
            }
            // 配置文件存在，读取返回
            return JsonUtil.readJsonFileToMap(configFile);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据任务类型创建默认配置（示例，按需修改）
     */
    private static Map<String, Object> createDefaultConfig(String taskType) {
        Map<String, Object> defaultConfig = new HashMap<>();
        switch (taskType.toLowerCase()) {
            case "baotu":
                defaultConfig.put("scanInterval", 5000);
                defaultConfig.put("maxTreasureCount", 20);
                defaultConfig.put("description", "宝图任务默认配置");
                defaultConfig.put("分图坐标", new Object[] {
                        Map.of("x", 100, "y", 200),
                        Map.of("x", 150, "y", 250)
                });
                break;
            case "shimen":
                defaultConfig.put("taskCount", 10);
                defaultConfig.put("description", "师门任务默认配置");
                break;
            default:
                defaultConfig.put("description", "默认任务配置");
        }
        return defaultConfig;
    }

    // 删除配置文件不变
    public static boolean deleteConfig(String deviceId, String taskType) {
        try {
            String configDir = ConfigUtil.getConfigDir();
            File configFile = new File(configDir, deviceId + "_" + taskType + ".json");
            if (configFile.exists()) {
                return configFile.delete();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
