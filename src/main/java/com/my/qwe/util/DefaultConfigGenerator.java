package com.my.qwe.util;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DefaultConfigGenerator {

    /**
     * 为设备生成配置文件（如果不存在），并初始化任务配置节（如果不存在）
     */
    public static void generate(String deviceName, List<String> taskTypes) {
        try {
            String safeName = deviceName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            File file = new File("D:/myapp/config/" + safeName + ".ini");

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            Wini ini = file.exists() ? new Wini(file) : new Wini();
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            for (String task : taskTypes) {
                if (!ini.containsKey(task)) {
                    ini.put(task, "描述", "无其他配置");
                }
            }

            ini.store(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
