package com.my.qwe.http;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class DeviceHttpClient {

    private final String baseUrl;
    private final String deviceId;

    public DeviceHttpClient(String baseUrl, String deviceId) {
        this.baseUrl = baseUrl;
        this.deviceId = deviceId;
    }

    private String postJson(String endpoint, JSONObject json) throws IOException {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
        conn.getOutputStream().write(data);

        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            return sb.toString();
        }
    }

    public List<int[]> findImage(String imageName, double similarity) throws IOException {
        JSONObject req = new JSONObject();
        req.put("fun", "find_image");
        req.put("msgid", 0);
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        JSONArray imgList = new JSONArray();
        JSONObject imgObj = new JSONObject();
        imgObj.put("img_name", imageName);
        imgObj.put("similar", similarity);
        imgList.put(imgObj);
        data.put("img_list", imgList);
        req.put("data", data);

        String resp = postJson("/api/find_image", req);
        JSONObject jsonResp = new JSONObject(resp);

        List<int[]> coords = new ArrayList<>();
        if (jsonResp.has("data")) {
            JSONArray arr = jsonResp.getJSONObject("data").optJSONArray("position_list");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONArray pos = arr.getJSONArray(i);
                    int x = pos.getInt(0);
                    int y = pos.getInt(1);
                    coords.add(new int[]{x, y});
                }
            }
        }
        return coords;
    }

    public boolean click(int x, int y) throws IOException {
        JSONObject req = new JSONObject();
        req.put("fun", "click");
        req.put("msgid", 0);
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("x", x);
        data.put("y", y);
        req.put("data", data);

        String resp = postJson("/api/click", req);
        JSONObject jsonResp = new JSONObject(resp);
        return jsonResp.optInt("code", -1) == 0;
    }

    public boolean moveMouse(int x, int y) throws IOException {
        JSONObject req = new JSONObject();
        req.put("fun", "move_mouse");
        req.put("msgid", 0);
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("x", x);
        data.put("y", y);
        req.put("data", data);

        String resp = postJson("/api/move_mouse", req);
        JSONObject jsonResp = new JSONObject(resp);
        return jsonResp.optInt("code", -1) == 0;
    }

    public String ocrRecognize(int x, int y, int width, int height) throws IOException {
        JSONObject req = new JSONObject();
        req.put("fun", "ocr");
        req.put("msgid", 0);
        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("x", x);
        data.put("y", y);
        data.put("width", width);
        data.put("height", height);
        req.put("data", data);

        String resp = postJson("/api/ocr", req);
        JSONObject jsonResp = new JSONObject(resp);
        if (jsonResp.has("data")) {
            return jsonResp.getJSONObject("data").optString("text", "");
        }
        return "";
    }

    public byte[] getDeviceScreenshot(boolean gzip, boolean binary, boolean isJpg, boolean original) throws IOException {
        JSONObject req = new JSONObject();
        req.put("fun", "get_device_screenshot");
        req.put("msgid", 0);

        JSONObject data = new JSONObject();
        data.put("deviceid", deviceId);
        data.put("gzip", gzip);
        data.put("binary", binary);
        data.put("isjpg", isJpg);
        data.put("original", original);

        req.put("data", data);

        String resp = postJson("/api/get_device_screenshot", req);
        JSONObject jsonResp = new JSONObject(resp);

        int status = jsonResp.optInt("status", -1);
        if (status != 0) {
            String message = jsonResp.optString("message", "未知错误");
            throw new IOException("截屏失败，错误码：" + status + "，信息：" + message);
        }

        JSONObject respData = jsonResp.getJSONObject("data");
        String base64Img = respData.optString("img", null);
        if (base64Img == null) {
            throw new IOException("截屏失败，未返回图片数据");
        }

        // 解码base64字符串为字节数组（图片数据）
        return Base64.getDecoder().decode(base64Img);
    }
}
