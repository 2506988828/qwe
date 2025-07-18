package com.my.qwe.task.config;

import org.ini4j.Wini;
import org.ini4j.Profile.Section;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class IniConfigLoader {

    // 原有加载配置方法（保持不变）
    public static Map<String, String> loadTaskConfig(String deviceId, String taskName) {
        Map<String, String> result = new HashMap<>();
        try {
            String path = "D:/myapp/config/" + deviceId + ".ini";
            File file = new File(path);

            if (!file.exists()) {
                System.err.println("配置文件不存在：" + path);
                return result;
            }

            Wini ini = new Wini(file);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            if (!ini.containsKey(taskName)) {
                System.err.println("配置文件中未找到任务 [" + taskName + "] 区块");
                return result;
            }

            for (String key : ini.get(taskName).keySet()) {
                String value = ini.get(taskName, key);
                result.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    // 新增：修改或新增配置项（兼容ini4j库，不使用addSection）
    public static boolean setTaskConfig(String deviceId, String taskName, String key, String value) {
        try {
            String path = "D:/myapp/config/" + deviceId + ".ini";
            File file = new File(path);

            // 确保目录存在
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            Wini ini = new Wini(file);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            // 关键：通过get方法获取区块，不存在时会自动创建（无需addSection）
            Section section = ini.get(taskName);
            section.put(key, value); // 设置配置项（覆盖或新增）

            ini.store(); // 保存到文件
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}