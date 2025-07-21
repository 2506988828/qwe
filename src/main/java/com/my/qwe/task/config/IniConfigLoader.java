package com.my.qwe.task.config;

import org.ini4j.Wini;
import org.ini4j.Profile.Section;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class IniConfigLoader {
    private final String deviceName;  // 改为deviceName
    private final String configPath;
    private Wini ini;

    /**
     * 构造函数，初始化特定设备的配置加载器（使用deviceName）
     */
    public IniConfigLoader(String deviceName) {
        this.deviceName = deviceName;
        this.configPath = "D:/myapp/config/" + deviceName + ".ini";  // 文件名改为deviceName.ini
        loadIniFile();
    }

    /**
     * 加载配置文件（路径基于deviceName）
     */
    private void loadIniFile() {
        try {
            File file = new File(configPath);

            // 确保目录存在
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            // 文件不存在则创建空文件
            if (!file.exists()) {
                file.createNewFile();
            }

            ini = new Wini(file);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败: " + configPath, e);
        }
    }

    /**
     * 获取指定区块的所有配置项
     */
    public Properties getSection(String sectionName) {
        Properties props = new Properties();
        Section section = ini.get(sectionName);

        if (section != null) {
            for (String key : section.keySet()) {
                props.put(key, section.get(key));
            }
        }

        return props;
    }

    /**
     * 设置配置项（自动创建区块）
     */
    public void setProperty(String sectionName, String key, String value) {
        Section section = ini.get(sectionName);

        if (section == null) {
            section = ini.add(sectionName);  // 兼容ini4j的区块创建方式
        }

        section.put(key, value);
    }

    /**
     * 清除指定区块的所有配置
     */
    public void clearSection(String sectionName) {
        Section section = ini.get(sectionName);

        if (section != null) {
            section.clear();
        }
    }

    /**
     * 保存配置到文件
     */
    public void save() {
        try {
            ini.store();
        } catch (IOException e) {
            throw new RuntimeException("保存配置文件失败: " + configPath, e);
        }
    }

    // 静态方法同步修改为使用deviceName
    public static Map<String, String> loadTaskConfig(String deviceName, String taskName) {  // 参数改为deviceName
        Map<String, String> result = new HashMap<>();
        try {
            String path = "D:/myapp/config/" + deviceName + ".ini";  // 路径改为deviceName.ini
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

    // 静态方法同步修改为使用deviceName
    public static boolean setTaskConfig(String deviceName, String taskName, String key, String value) {  // 参数改为deviceName
        try {
            String path = "D:/myapp/config/" + deviceName + ".ini";  // 路径改为deviceName.ini
            File file = new File(path);

            // 确保目录存在
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            Wini ini = new Wini(file);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            // 获取区块（不存在时自动创建）
            Section section = ini.get(taskName);
            section.put(key, value);

            ini.store();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}