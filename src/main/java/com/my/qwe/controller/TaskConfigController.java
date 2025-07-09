package com.my.qwe.controller;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TaskConfigController {

    private static final String CONFIG_DIR = "D:/myapp/config";

    /**
     * 加载某个设备某个任务的配置文件
     *
     * 示例：
     *   Wini ini = TaskConfigController.loadConfig("dev1", "baotu");
     */
    public static Wini loadConfig(String deviceId, String taskType) throws IOException {
        File file = new File(CONFIG_DIR, deviceId + "_" + taskType + ".ini");
        if (!file.exists()) {
            file.createNewFile();
        }

        Wini ini = new Wini(file);
        ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);
        return ini;
    }

    /**
     * 获取指定 section 的所有键值对
     *
     * 示例：
     *   Map<String, String> values = TaskConfigController.getSection(ini, "坐标");
     */
    public static Map<String, String> getSection(Wini ini, String sectionName) {
        Map<String, String> result = new HashMap<>();
        if (ini.containsKey(sectionName)) {
            for (String key : ini.get(sectionName).keySet()) {
                result.put(key, ini.get(sectionName, key));
            }
        }
        return result;
    }

    /**
     * 获取指定 section 中某个键的值
     *
     * 示例：
     *   String value = TaskConfigController.get(ini, "坐标", "baotu_1");
     */
    public static String get(Wini ini, String section, String key) {
        return ini.get(section, key);
    }

    /**
     * 设置指定 section 的某个键值
     * 如果 section 不存在，会自动创建
     *
     * 示例：
     *   TaskConfigController.set(ini, "坐标", "baotu_1", "123,456");
     */
    public static void set(Wini ini, String section, String key, String value) throws IOException {
        ini.put(section, key, value);
        ini.store();
    }

    /**
     * 删除指定 section 中的某个键
     *
     * 示例：
     *   TaskConfigController.deleteKey(ini, "坐标", "baotu_1");
     */
    public static void deleteKey(Wini ini, String section, String key) throws IOException {
        if (ini.containsKey(section) && ini.get(section).containsKey(key)) {
            ini.remove(section, key);
            ini.store();
        }
    }

    /**
     * 删除整个 section
     *
     * 示例：
     *   TaskConfigController.deleteSection(ini, "临时区域");
     */
    public static void deleteSection(Wini ini, String section) throws IOException {
        ini.remove(section);
        ini.store();
    }

    /**
     * 创建新的 section（如果已经存在不会报错）
     *
     * 示例：
     *   TaskConfigController.createSection(ini, "新区域");
     */
    public static void createSection(Wini ini, String sectionName) throws IOException {
        ini.add(sectionName);
        ini.store();
    }

    /**
     * 手动保存配置（如果只修改了内存中的 Wini，可调用此方法落盘）
     *
     * 示例：
     *   TaskConfigController.save(ini);
     */
    public static void save(Wini ini) throws IOException {
        ini.store();
    }
}
