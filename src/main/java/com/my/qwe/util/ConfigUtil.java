package com.my.qwe.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class ConfigUtil {

    private static final String BASE_DIR = "D:/myapp";  // 或从环境变量加载
    private static final String CONFIG_DIR = BASE_DIR + "/config";
    private static final String CONFIG_PATH = CONFIG_DIR + "/human_config.ini";

    /**
     * 获取配置文件目录路径
     */
    public static String getConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // 自动创建目录
        }
        return CONFIG_DIR;
    }

    /**
     * 获取日志目录路径
     */
    public static String getLogDir() {
        File dir = new File(BASE_DIR + "/log");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    /**
     * 获取截图目录路径
     */
    public static String getScreenshotDir() {
        File dir = new File(BASE_DIR + "/screenshots");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    public static Map<String, Object> loadHumanLikeConfig() {
        try {
            File configFile = new File(CONFIG_PATH);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new FileInputStream(configFile), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int[] parseIntArray(String csv) {
        if (csv == null || csv.isBlank()) return new int[0];
        String[] parts = csv.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i].trim());
        }
        return result;
    }



    // 如果你有更多用途（比如任务记录、缓存目录等）也可以加
}
