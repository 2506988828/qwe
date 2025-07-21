package com.my.qwe.http;

import com.my.qwe.model.DeviceInfo;
import com.my.qwe.task.TaskStepNotifier;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Array;
import java.util.*;

import static org.bytedeco.opencv.global.opencv_core.CV_32FC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;
import static org.bytedeco.opencv.global.opencv_imgproc.TM_CCOEFF_NORMED;
import static org.bytedeco.opencv.global.opencv_imgproc.matchTemplate;

/**
 * 设备相关 HTTP 接口客户端封装
 * 调用本地设备管理服务，支持设备列表、截图、查图、OCR、点击等操作
 */
public class DeviceHttpClient {

    private static final String API_URL = "http://127.0.0.1:9912/api";





    /**
     * 获取设备列表
     * @return 设备信息列表
     * @throws IOException 网络或接口异常
     */
    public static List<DeviceInfo> getDeviceList() throws IOException {
        JSONObject req = new JSONObject();
        req.put("fun", "get_device_list");
        req.put("msgid", 0);
        req.put("data", new JSONObject());

        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.optInt("status", -1) != 0) {
            throw new IOException("获取设备列表失败：" + resp.optString("message"));
        }

        JSONObject data = resp.getJSONObject("data");
        List<DeviceInfo> devices = new ArrayList<>();

        for (String key : data.keySet()) {
            JSONObject d = data.getJSONObject(key);
            DeviceInfo info = new DeviceInfo();
            info.deviceId = d.optString("deviceid");
            info.deviceName = d.optString("device_name");
            info.ip = d.optString("ip");
            info.username = d.optString("username");
            info.model = d.optString("model");
            info.state = d.optInt("state");
            info.width = d.optString("width");
            info.height = d.optString("height");
            info.name = d.optString("name");
            devices.add(info);
        }

        return devices;
    }

    // 其他已有方法...

    /**
     * 多点找色
     * @param deviceId 设备ID
     * @param x1 区域左上角的X坐标
     * @param y1 区域左上角的Y坐标
     * @param x2 区域右下角的X坐标
     * @param y2 区域右下角的Y坐标
     * @param firstColor 要查找的首色，格式为 "RRGGBB-DRDGDB|..."
     * @param offsetColor 偏移色，多个颜色格式 "RRGGBB-DRDGDB|..."
     * @param similarity 匹配相似度阈值（0~1）
     * @param dir 查找方向 (0:从左到右, 1:从左到右从下到上, 2:从右到左从上到下, 3:从右到左从下到上)
     * @return 查找结果，坐标数组 [x, y] 或错误信息
     * @throws IOException 网络或接口异常
     */
    public static int[] findMultiColor(String deviceId, int x1, int y1, int x2, int y2,
                                       String firstColor, String offsetColor,
                                       double similarity, int dir) throws IOException {
        // 构造请求数据
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("x1", x1);
        data.put("y1", y1);
        data.put("x2", x2);
        data.put("y2", y2);
        data.put("first_color", firstColor);
        data.put("offset_color", offsetColor);
        data.put("similarity", similarity);
        data.put("dir", dir);

        // 构建请求体
        JSONObject req = new JSONObject();
        req.put("fun", "find_multi_color");
        req.put("msgid", 0);
        req.put("data", data);

        // 发送请求
        JSONObject resp = HttpJsonClient.post(API_URL, req);

        // 检查 HTTP 接口返回状态
        if (resp.optInt("status", -1) != 0) {
            throw new IOException("查找多点颜色接口调用失败: " + resp.optString("message"));
        }

        // 获取 data 部分
        JSONObject dataResp = resp.optJSONObject("data");
        /*if (dataResp == null || dataResp.optInt("code", 1) != 0) {
            throw new IOException("颜色查找失败，设备ID: " + deviceId + ", 错误信息: " + resp.optString("message"));
        }*/

        // 提取结果坐标
        JSONArray result = dataResp.optJSONArray("result");

            int x = result.getInt(0);
            int y = result.getInt(1);
            return new int[]{x, y};

    }


    /**
     * 获取设备屏幕截图，返回base64格式的图片字符串（JPEG格式）
     * @param deviceId 设备ID
     * @return Base64编码的图片字符串
     * @throws IOException 网络或接口异常
     */
    public static String getScreenshot(String deviceId) throws IOException {
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("isjpg", true);
        data.put("original", false);
        data.put("gzip", false);
        data.put("binary", false);

        JSONObject req = new JSONObject();
        req.put("fun", "get_device_screenshot");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.optInt("status", -1) != 0) {
            throw new IOException("截图失败：" + resp.optString("message"));
        }

        return resp.getJSONObject("data").optString("img");
    }

    /**
     * 在指定区域查找图片
     * @param deviceId 设备ID
     * @param rect 查找区域，二维数组 [[x1,y1],[x2,y2],...]
     * @param filename 图片文件路径
     * @param similarity 匹配相似度阈值，0~1之间
     * @return 匹配到的第一个坐标[x,y]
     * @throws IOException 网络或接口异常，或者未找到匹配图片
     */
    public static int[] findImage(String deviceId, int[] rect, String filename, double similarity) throws IOException {
        String imgPath = "D:\\myapp\\images\\"+filename+".bmp";
        byte[] imageBytes = Files.readAllBytes(Paths.get(imgPath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        JSONArray rectArray = new JSONArray();
        rectArray.put(new JSONArray(Arrays.asList(rect[0], rect[1])));  // 左上角
        rectArray.put(new JSONArray(Arrays.asList(rect[0], rect[3])));  // 左下角
        rectArray.put(new JSONArray(Arrays.asList(rect[2], rect[1])));  // 右上角
        rectArray.put(new JSONArray(Arrays.asList(rect[2], rect[3])));  // 右下角
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("rect", new JSONArray(rectArray));
        data.put("img", base64Image);
        data.put("similarity", similarity);

        JSONObject req = new JSONObject();
        req.put("fun", "find_image");
        req.put("msgid", 0);
        req.put("data", data);


        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.optInt("status", -1) != 0) {
            throw new IOException("查找图片失败：" + resp.optString("message"));
        }

        JSONArray pos = resp.getJSONObject("data").optJSONArray("result");
        if (pos == null || pos.length() < 2) {
            return  new int[]{-1, -1};
        }
        if(resp.getJSONObject("data").optInt("code")==1) {
            return new int[]{-1, -1};

        }
        return new int[]{pos.getInt(0), pos.getInt(1)};
    }
    /**
     * 在指定区域查找图片
     * @param deviceId 设备ID
     * @param filename 图片文件路径
     * @param similarity 匹配相似度阈值，0~1之间
     * @return 匹配到的第一个坐标[x,y]
     * @throws IOException 网络或接口异常，或者未找到匹配图片
     */
    public static int[] findImage(String deviceId, String filename, double similarity) throws IOException {

        String imgPath = "D:\\myapp\\images\\" + filename + ".bmp";

        byte[] imageBytes = Files.readAllBytes(Paths.get(imgPath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);



        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("img", base64Image);
        data.put("similarity", similarity);


        JSONObject req = new JSONObject();

        req.put("fun", "find_image");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.getInt("status")!=0) {
            System.out.println("查找失败");
        }

        int code = resp.getJSONObject("data").getInt("code");
        if ( code ==1 ){
            return new int[]{-1, -1};
        }
        else if ( code !=1){
            JSONArray ja1= resp.getJSONObject("data").getJSONArray("result");
            int[] result = new int[ja1.length()];
            for (int i = 0; i < ja1.length(); i++) {
                result[i] = ja1.getInt(i); // 自动处理类型转换
            }
            return result;
        }
        return new int[]{-1, -1};
}


    /*查找多张图片*/

    public static JSONObject findImages(String deviceId, String imageFilename,String img2, double similarity) {

        String imgPath = "D:\\myapp\\images\\"+imageFilename+".bmp";
        String imgPath2 = "D:\\myapp\\images\\"+img2+".bmp";
        try {
            JSONObject request = new JSONObject();
            request.put("fun", "find_image_ex");
            request.put("msgid", 0);

            // 准备图片Base64字符串
            byte[] imageBytes = Files.readAllBytes(Paths.get(imgPath));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            byte[] imageBytes2 = Files.readAllBytes(Paths.get(imgPath2));
            String base64Image2 = Base64.getEncoder().encodeToString(imageBytes2);

            JSONArray ja= new JSONArray().put(base64Image);
            ja.put(base64Image2);
            // 构建请求数据
            JSONObject data = new JSONObject();
            data.put("deviceid", deviceId);
            data.put("img_list", ja);
            data.put("all", false); // false表示找到一张就返回
            data.put("repeat", false); // 是否查找重复图片

            data.put("similarity", similarity);

            request.put("data", data);



            // 发送请求并获取响应
            JSONObject resp = HttpJsonClient.post(API_URL, request);

            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对指定区域执行OCR识别，返回识别文字
     * @param deviceId 设备ID
     * @param rect 识别区域，格式 [x, y, width, height]
     * @return 识别出的文字，识别失败返回空字符串
     * @throws IOException 网络或接口异常
     */
    public static String ocr(String deviceId, int[] rect) throws IOException {
        // 构建请求参数
        JSONObject data = new JSONObject();
        JSONArray rectArray = new JSONArray();
        rectArray.put(new JSONArray(Arrays.asList(rect[0], rect[1])));  // 左上角
        rectArray.put(new JSONArray(Arrays.asList(rect[0], rect[3])));  // 左下角
        rectArray.put(new JSONArray(Arrays.asList(rect[2], rect[1])));  // 右上角
        rectArray.put(new JSONArray(Arrays.asList(rect[2], rect[3])));  // 右下角

        data.put("deviceid", deviceId);
        data.put("rect", rectArray);

        JSONObject req = new JSONObject();
        req.put("fun", "ocr");
        req.put("msgid", 0);
        req.put("data", data);
        req.put("isjpg", true);

        // 发送请求并获取响应
        JSONObject resp = HttpJsonClient.post(API_URL, req);

        // 检查响应状态
        if (resp.optInt("status", -1) != 0) {
            throw new IOException("OCR识别失败：" + resp.optString("message"));
        }

        // 安全解析OCR结果
        try {
            JSONObject dataObj = resp.getJSONObject("data");
            JSONArray listArray = dataObj.getJSONArray("list");

            if (listArray.length() > 0) {
                return listArray.getJSONObject(0).getString("txt");
            } else {
                TaskStepNotifier.notifyStep(deviceId, "OCR识别结果列表为空");
                return "";
            }
        } catch (JSONException e) {
            TaskStepNotifier.notifyStep(deviceId, "解析OCR结果失败：" + e.getMessage());
            return "";
        }
    }

    /**
     * 对指定区域执行OCR识别，返回识别文字
     * @param deviceId 设备ID
     * @param rect 识别区域，格式 [x, y, width, height]
     * @return 识别出的文字，识别失败返回空字符串
     * @throws IOException 网络或接口异常
     */
    public static String ocrEx(String deviceId, int[] rect) throws IOException {
        // 构建请求参数
        JSONObject data = new JSONObject();
        JSONArray rectArray = new JSONArray();
        rectArray.put(new JSONArray(Arrays.asList(rect[0], rect[1])));  // 左上角
        rectArray.put(new JSONArray(Arrays.asList(rect[0], rect[3])));  // 左下角
        rectArray.put(new JSONArray(Arrays.asList(rect[2], rect[1])));  // 右上角
        rectArray.put(new JSONArray(Arrays.asList(rect[2], rect[3])));  // 右下角

        data.put("deviceid", deviceId);
        data.put("rect", rectArray);

        JSONObject req = new JSONObject();
        req.put("fun", "ocr");
        req.put("msgid", 0);
        req.put("data", data);
        req.put("isjpg", true);

        // 发送请求并获取响应
        JSONObject resp = HttpJsonClient.post(API_URL, req);

        // 检查响应状态
        if (resp.optInt("status", -1) != 0) {
            throw new IOException("OCR识别失败：" + resp.optString("message"));
        }

        // 安全解析OCR结果
        try {
            JSONObject dataObj = resp.getJSONObject("data");
            JSONArray listArray = dataObj.getJSONArray("list");

            if (listArray.length() > 0) {
                return listArray.getJSONObject(0).getString("txt");
            } else {
                TaskStepNotifier.notifyStep(deviceId, "OCR识别结果列表为空");
                return "";
            }
        } catch (JSONException e) {
            TaskStepNotifier.notifyStep(deviceId, "解析OCR结果失败：" + e.getMessage());
            return "";
        }
    }

    /**
     * 点击屏幕指定坐标
     * @param deviceId 设备ID
     * @param x X坐标
     * @param y Y坐标
     * @throws IOException 网络或接口异常
     */
    public static void tap(String deviceId, int x, int y) throws IOException {
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("x", x);
        data.put("y", y);

        JSONObject req = new JSONObject();
        req.put("fun", "tap");
        req.put("msgid", 0);
        req.put("data", data);



        JSONObject resp = HttpJsonClient.post(API_URL, req);


        if (resp.optInt("status", -1) != 0) {
            throw new IOException("点击操作失败：" + resp.optString("message"));
        }
    }

    /**
     * 滑动操作
     * @param deviceId 设备ID
     * @param x1 起点X坐标
     * @param y1 起点Y坐标
     * @param x2 终点X坐标
     * @param y2 终点Y坐标
     * @param duration 持续时间，单位毫秒
     * @throws IOException 网络或接口异常
     */
    public static void swipe(String deviceId, int x1, int y1, int x2, int y2, int duration) throws IOException {
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("x1", x1);
        data.put("y1", y1);
        data.put("x2", x2);
        data.put("y2", y2);
        data.put("duration", duration);

        JSONObject req = new JSONObject();
        req.put("fun", "swipe");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);
        if (resp.optInt("status", -1) != 0) {
            throw new IOException("滑动操作失败：" + resp.optString("message"));
        }
    }

    /**
     * 获取裁剪区域图片，返回base64编码的图片字符串
     * @param deviceId 设备ID
     * @param rect 裁剪区域 [x,y,width,height]
     * @return Base64编码图片字符串
     * @throws IOException 网络或接口异常
     */
    public static String getCropImage(String deviceId, int[] rect) throws IOException {
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("rect", new JSONArray(rect));

        JSONObject req = new JSONObject();
        req.put("fun", "get_crop_image");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.optInt("status", -1) != 0) {
            throw new IOException("获取裁剪图片失败：" + resp.optString("message"));
        }

        return resp.getJSONObject("data").optString("img");
    }

    /**
     * 工具方法：将二维int数组转成JSONArray嵌套数组
     * @param arr 二维数组
     * @return JSONArray对象
     */
    private static JSONArray toJSONArray(int[][] arr) {
        JSONArray jsonArray = new JSONArray();
        for (int[] subArr : arr) {
            JSONArray subJson = new JSONArray();
            for (int val : subArr) {
                subJson.put(val);
            }
            jsonArray.put(subJson);
        }
        return jsonArray;
    }

    /**
     * 模拟鼠标单击操作
     * @param deviceId 设备ID
     * @param button 鼠标按键，"left" 或 "right"，不填默认为 "left"
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     *
     * @throws IOException 网络或接口异常
     */
    public static void click(String deviceId, String button, int x, int y) throws IOException {

        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("button", button == null || button.isEmpty() ? "left" : button);
        data.put("x", x);
        data.put("y", y);
        int time = (int)(Math.random() * 80);
        data.put("time", time);

        JSONObject req = new JSONObject();
        req.put("fun", "click");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.optInt("status", -1) != 0) {
            throw new IOException("鼠标单击失败：" + resp.optString("message"));
        }
    }


    /**
     * 模拟鼠标单击操作
     * @param deviceId 设备ID
     * @param x 屏幕X坐标
     * @param y 屏幕Y坐标
     *
     * @throws IOException 网络或接口异常
     */
    public static void movemouse(String deviceId, int x, int y) throws IOException {

        JSONObject data = new JSONObject();
        data.put("y", y);
        data.put("x", x);
        data.put("deviceid", deviceId);


        JSONObject req = new JSONObject();
        req.put("fun", "mouse_move");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);

        if (resp.optInt("status", -1) != 0) {
            throw new IOException("鼠标移动失败：" + resp.optString("message"));
        }
    }

    public static void  sendkey(String deviceId, String key) throws IOException {
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("key", key);
        data.put("fn_key", "");
        JSONObject req = new JSONObject();
        req.put("fun", "send_key");
        req.put("msgid", 0);
        req.put("data", data);
        JSONObject resp = HttpJsonClient.post(API_URL, req);
        if (resp.optInt("status", -1) != 0) {
            throw new IOException("键盘输入失败：" + resp.optString("message"));
        }

    }



    public static void doubleclick(String deviceId, String button, int x, int y) throws IOException {

        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("button", button == null || button.isEmpty() ? "left" : button);
        data.put("x", x);
        data.put("y", y);
        int time = (int)(Math.random() * 50);
        data.put("time", time);

        JSONObject req = new JSONObject();
        req.put("fun", "click");
        req.put("msgid", 0);
        req.put("data", data);

        JSONObject resp = HttpJsonClient.post(API_URL, req);
        HttpJsonClient.post(API_URL, req);
        if (resp.optInt("status", -1) != 0) {
            throw new IOException("鼠标单击失败：" + resp.optString("message"));
        }
    }


    public static String getScreenshotBase64(String deviceId) {
        // 构造请求 JSON
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
            // 发送 HTTP 请求
            JSONObject response = HttpJsonClient.post(API_URL, request);

            if (response.getInt("status") == 0) {
                String img = response.getJSONObject("data").getString("img");

                // 去除 data:image/jpeg;base64, 前缀（如果有）
                if (img.contains(",")) {
                    img = img.substring(img.indexOf(",") + 1);
                }

                // 去掉空格或换行
                img = img.replaceAll("\\s+", "");

                return img;
            } else {
                System.err.println("截图失败：" + response.optString("message"));
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //在整个屏幕找所有符合图片的坐标点
/*
    调用方式
    List<int[]>toal = DeviceHttpClient.findAllMatches(deviceId,"D:\\myapp\\images\\shuzi4.bmp", 0.8F,10);

        for (int[] arr : toal) {
        System.out.println(Arrays.toString(arr));
    }*/
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
            System.out.println(templateImagePath);

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
