package com.my.qwe.task.config;

import com.my.qwe.util.ConfigUtil;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TaskConfigLoader {

    /**
     * 读取指定设备和任务类型的配置文件（INI 格式）。
     * 如果配置文件不存在，会自动创建一个默认配置并返回。
     */
    public static Map<String, Map<String, String>> loadConfig(String name, String taskType) {
        try {
            String configDir = ConfigUtil.getConfigDir();
            File configFile = new File(configDir, name + "_" + taskType + ".ini");

            if (!configFile.exists()) {
                System.out.println("配置文件不存在，自动创建默认配置文件：" + configFile.getAbsolutePath());
                Map<String, Map<String, String>> defaultConfig = createDefaultConfig(taskType);
                try {
                    saveConfigToFile(defaultConfig, configFile);
                    System.out.println("默认配置文件创建成功");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("默认配置文件创建失败：" + e.getMessage());
                }
                return defaultConfig;
            }

            return readConfigFromFile(configFile);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建默认配置（按任务类型生成默认配置）
     */
    private static Map<String, Map<String, String>> createDefaultConfig(String taskType) {
        Map<String, Map<String, String>> config = new HashMap<>();

        switch (taskType.toLowerCase()) {
            case "baotu":
                config.put("meta", Map.of(
                        "description", "宝图任务默认配置",
                        "备注", "这是一个中文字段测试"
                ));
                config.put("step", Map.of(
                        "扫描间隔", "5000",
                        "最大宝图数", "20"
                ));
                break;

            case "shimen":
                config.put("meta", Map.of(
                        "description", "师门任务默认配置"
                ));
                break;

            default:
                config.put("meta", Map.of("description", "默认任务配置"));
                break;
        }

        return config;
    }

    /**
     * 保存配置到 INI 文件
     */
    private static void saveConfigToFile(Map<String, Map<String, String>> config, File file) throws IOException {
        // 确保目录存在
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new IOException("无法创建配置文件目录: " + parentDir.getAbsolutePath());
            } else {
                System.out.println("成功创建配置目录: " + parentDir.getAbsolutePath());
            }
        }

        // 确保文件存在，创建空文件
        if (!file.exists()) {
            boolean createdFile = file.createNewFile();
            if (!createdFile) {
                throw new IOException("无法创建配置文件: " + file.getAbsolutePath());
            } else {
                System.out.println("成功创建配置文件: " + file.getAbsolutePath());
            }
        }

        Wini ini = new Wini(file);
        ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

        for (Map.Entry<String, Map<String, String>> sectionEntry : config.entrySet()) {
            String section = sectionEntry.getKey();
            Map<String, String> values = sectionEntry.getValue();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                ini.put(section, entry.getKey(), entry.getValue());
            }
        }

        ini.store();

        System.out.println("配置文件已保存: " + file.getAbsolutePath());
    }

    /**
     * 从 INI 文件中读取配置
     */
    private static Map<String, Map<String, String>> readConfigFromFile(File file) throws IOException {
        Wini ini = new Wini(file);
        ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

        Map<String, Map<String, String>> config = new HashMap<>();

        for (String section : ini.keySet()) {
            Map<String, String> sectionMap = new HashMap<>();
            for (String key : ini.get(section).keySet()) {
                sectionMap.put(key, ini.get(section, key));
            }
            config.put(section, sectionMap);
        }

        System.out.println("配置文件已读取: " + file.getAbsolutePath());

        return config;
    }

    /**
     * 删除指定设备和任务类型的配置文件
     */
    public static boolean deleteConfig(String deviceId, String taskType) {
        try {
            String configDir = ConfigUtil.getConfigDir();
            File configFile = new File(configDir, deviceId + "_" + taskType + ".ini");
            if (configFile.exists()) {
                boolean deleted = configFile.delete();
                if (deleted) {
                    System.out.println("配置文件已删除: " + configFile.getAbsolutePath());
                } else {
                    System.err.println("配置文件删除失败: " + configFile.getAbsolutePath());
                }
                return deleted;
            }
            System.out.println("配置文件不存在，无需删除: " + configFile.getAbsolutePath());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
