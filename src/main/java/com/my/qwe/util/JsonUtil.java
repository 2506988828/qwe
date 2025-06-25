package com.my.qwe.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> readJsonFileToMap(File file) throws IOException {
        return mapper.readValue(file, Map.class);
    }

    public static void writeMapToJsonFile(Map<String, Object> map, File file) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, map);
    }
}
