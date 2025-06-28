package com.my.qwe.controller;

import com.my.qwe.http.HttpJsonClient;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class ImageMatchService {

    private static final String API_URL = "http://127.0.0.1:9912/api";

    /**
     * 截屏并查找所有符合匹配度的点（不限制数量）
     * 返回List<int[]>，每个int[]包含两个元素[x,y]
     */
    public static List<int[]> findAllMatches(String deviceId, String templateImagePath, float threshold, int minDistance) {
        try {
            // 1. 构造HTTP请求获取截图
            JSONObject data = new JSONObject();
            data.put("deviceid", deviceId);
            data.put("gzip", false);
            data.put("binary", false);
            data.put("isjpg", true);
            data.put("original", false);

            JSONObject req = new JSONObject();
            req.put("fun", "get_device_screenshot");
            req.put("msgid", 0);
            req.put("data", data);

            JSONObject resp = HttpJsonClient.post(API_URL, req);
            if (resp.getInt("status") != 0) {
                System.err.println("截图失败：" + resp.optString("message"));
                return new ArrayList<>();
            }

            // 2. base64 解码为 Mat
            String base64 = resp.getJSONObject("data").getString("img");
            byte[] decoded = Base64.getMimeDecoder().decode(base64);
            Mat screenMat = imdecode(new Mat(new BytePointer(decoded)), IMREAD_COLOR);
            if (screenMat.empty()) {
                System.err.println("设备截图解码失败");
                return new ArrayList<>();
            }

            // 3. 读取模板图
            Mat template = imread(templateImagePath, IMREAD_COLOR);
            if (template.empty()) {
                System.err.println("模板图片读取失败：" + templateImagePath);
                return new ArrayList<>();
            }

            // 4. 模板匹配
            int resultCols = screenMat.cols() - template.cols() + 1;
            int resultRows = screenMat.rows() - template.rows() + 1;
            Mat result = new Mat(resultRows, resultCols, CV_32FC1);
            matchTemplate(screenMat, template, result, TM_CCOEFF_NORMED);

            // 5. 遍历所有位置，找到符合的点
            FloatIndexer indexer = result.createIndexer();
            List<int[]> points = new ArrayList<>();

            for (int y = 0; y < result.rows(); y++) {
                for (int x = 0; x < result.cols(); x++) {
                    float score = indexer.get(y, x);
                    if (score >= threshold) {
                        int centerX = x + template.cols() / 2;
                        int centerY = y + template.rows() / 2;
                        int[] candidate = new int[]{centerX, centerY};
                        if (!isTooClose(candidate, points, minDistance)) {
                            points.add(candidate);
                        }
                    }
                }
            }
            indexer.release();

            return points;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 判断当前点是否离已匹配点太近（避免重复）
    private static boolean isTooClose(int[] p, List<int[]> existing, int minDist) {
        for (int[] ep : existing) {
            double dx = p[0] - ep[0];
            double dy = p[1] - ep[1];
            if (Math.sqrt(dx * dx + dy * dy) < minDist) return true;
        }
        return false;
    }
}
