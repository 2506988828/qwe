package com.my.qwe.util;

import com.my.qwe.http.HttpJsonClient;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import org.json.JSONObject;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ImageMatchUtil {

    private static final String API_URL = "http://127.0.0.1:9912/api";

    // -------- 截屏接口调用，返回Base64 --------
    public static String getScreenshotBase64(String deviceId) {
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("gzip", false);
        data.put("binary", false);
        data.put("isjpg", true);
        data.put("original", false);

        JSONObject request = new JSONObject();
        request.put("fun", "get_device_screenshot");
        request.put("msgid", 0);
        request.put("data", data);

        try {
            JSONObject response = HttpJsonClient.post(API_URL, request);
            if (response.getInt("status") == 0) {
                String img = response.getJSONObject("data").getString("img");
                if (img.contains(",")) img = img.substring(img.indexOf(",") + 1);
                return img.replaceAll("\\s+", "");
            } else {
                System.err.println("截图失败：" + response.optString("message"));
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -------- Base64转Mat --------
    public static Mat base64ToMat(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) {
            return new Mat();
        }
        if (base64Str.contains(",")) {
            base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
        }
        base64Str = base64Str.replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(base64Str);
        Mat buf = new Mat(1, decoded.length, opencv_core.CV_8UC1);
        buf.data().put(decoded);
        return opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR);
    }

    // -------- 本地模板图片读取（字节流方式） --------
    public static Mat loadTemplateImage(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            Mat buf = new Mat(1, bytes.length, opencv_core.CV_8UC1);
            buf.data().put(bytes);
            return opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR);
        } catch (IOException e) {
            e.printStackTrace();
            return new Mat();
        }
    }

    // -------- 多目标模板匹配，返回所有匹配点左上角坐标 --------
    public static List<int[]> findAllTemplateMatches(Mat source, Mat template, double threshold) {
        List<int[]> matchPoints = new ArrayList<>();
        if (source.empty() || template.empty()) return matchPoints;

        int resultCols = source.cols() - template.cols() + 1;
        int resultRows = source.rows() - template.rows() + 1;
        if (resultCols <= 0 || resultRows <= 0) return matchPoints;

        Mat result = new Mat(resultRows, resultCols, opencv_core.CV_32FC1);
        opencv_imgproc.matchTemplate(source, template, result, opencv_imgproc.TM_CCOEFF_NORMED);

        FloatRawIndexer indexer = result.createIndexer();
        int w = result.cols();
        int h = result.rows();

        int nmsRadiusX = template.cols() / 2;
        int nmsRadiusY = template.rows() / 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float val = indexer.get(y, x);
                if (val >= threshold) {
                    boolean isLocalMax = true;
                    int startX = Math.max(0, x - nmsRadiusX);
                    int endX = Math.min(w - 1, x + nmsRadiusX);
                    int startY = Math.max(0, y - nmsRadiusY);
                    int endY = Math.min(h - 1, y + nmsRadiusY);
                    for (int ny = startY; ny <= endY && isLocalMax; ny++) {
                        for (int nx = startX; nx <= endX; nx++) {
                            if (ny == y && nx == x) continue;
                            if (indexer.get(ny, nx) > val) {
                                isLocalMax = false;
                                break;
                            }
                        }
                    }
                    if (isLocalMax) {
                        matchPoints.add(new int[]{x, y});
                    }
                }
            }
        }
        indexer.release();
        result.release();
        return matchPoints;
    }

    // -------- 主示例 --------
    public static void main(String[] args) {
        String deviceId = "76:90:DE:1F:12:2A"; // 你的设备ID
        String templatePath = "D:/myapp/images/数字6.bmp"; // 模板图路径
        double threshold = 0.85; // 匹配阈值（根据需要调整）

        // 1. 获取截图Base64
        String base64Screenshot = getScreenshotBase64(deviceId);
        if (base64Screenshot == null) {
            System.err.println("获取截图失败");
            return;
        }

        // 2. Base64转Mat
        Mat screenshotMat = base64ToMat(base64Screenshot);
        if (screenshotMat.empty()) {
            System.err.println("截图转换失败");
            return;
        }

        // 3. 读取模板图片
        Mat templateMat = loadTemplateImage(templatePath);
        if (templateMat.empty()) {
            System.err.println("模板图加载失败");
            return;
        }

        // 4. 执行多目标模板匹配
        List<int[]> matches = findAllTemplateMatches(screenshotMat, templateMat, threshold);
        if (matches.isEmpty()) {
            System.out.println("未找到匹配点");
        } else {
            System.out.println("找到匹配点坐标：");
            for (int[] pt : matches) {
                System.out.printf("x=%d, y=%d%n", pt[0], pt[1]);
            }
        }
    }
}
