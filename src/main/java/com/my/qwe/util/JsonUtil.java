package com.my.qwe.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> readJsonFileToMap(File file) throws IOException {

        return mapper.readValue(file, Map.class);
    }

    public static void writeMapToJsonFile(Map<String, Object> map, File file) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, map);
    }


    public static int[] parseRect(Object rectConfig) {
        // 解析出 OCR 区域的矩形坐标
        if (rectConfig instanceof List) {
            List<?> rectList = (List<?>) rectConfig;
            return new int[] {
                    (Integer) rectList.get(0), // left
                    (Integer) rectList.get(1), // top
                    (Integer) rectList.get(2), // right
                    (Integer) rectList.get(3)  // bottom
            };
        }
        return new int[0]; // 如果配置不正确，返回空数组
    }



}
