package com.my.qwe.init;

import java.io.File;

public class AppInitializer {
    private static final String BASE_DIR = "D:/myapp";

    private static final String[] REQUIRED_DIRS = {
            "config", "logs", "records", "images"
    };

    public static void initDirectories() {
        for (String dir : REQUIRED_DIRS) {
            File folder = new File(BASE_DIR, dir);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (created) {
                    System.out.println("创建目录: " + folder.getAbsolutePath());
                }
            }
        }
    }
}
