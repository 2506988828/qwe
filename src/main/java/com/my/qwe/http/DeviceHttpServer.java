package com.my.qwe.http;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class DeviceHttpServer {

    public static void main(String[] args) throws Exception {
        // 启动 HTTP 服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(9912), 0);
        server.createContext("/api", new MockApiHandler());  // 模拟接口路径
        server.setExecutor(null); // 创建默认的 Executor
        server.start();
        System.out.println("Server started at http://127.0.0.1:9912");
    }

    // 模拟的 API 处理器
    static class MockApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";

            // 检查请求方法
            if ("POST".equals(exchange.getRequestMethod())) {
                // 读取请求体 (请求可以是空的，主要是触发接口)
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);


                // 如果接口是“get_device_list”，我们模拟设备列表返回
                if (requestBody.contains("\"fun\":\"get_device_list\"")) {
                    response = simulateDeviceListResponse();
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    response = "{\"status\": -1, \"message\": \"Unknown API\"}";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                }
            } else {
                response = "{\"status\": -1, \"message\": \"Invalid request method\"}";
                exchange.sendResponseHeaders(405, response.getBytes().length);  // Method Not Allowed
            }

            // 写响应
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        // 模拟设备列表返回的 JSON 数据
        private String simulateDeviceListResponse() {
            JSONObject response = new JSONObject();
            response.put("status", 0);
            response.put("message", "成功");

            // 模拟设备数据
            JSONObject data = new JSONObject();

            // 假设有五个设备
            for (int i = 1; i <= 5; i++) {
                JSONObject device = new JSONObject();
                device.put("deviceid", "00:1A:2B:3C:4D:5" + i);
                device.put("device_name", "dev" + i);
                device.put("ip", "192.168.1." + (100 + i));
                device.put("username", "user" + i);
                device.put("model", "Model" + i);
                device.put("state", 1);  // 假设设备状态为正常
                device.put("width", "1920");
                device.put("height", "1080");
                device.put("name", "dev" + i);

                data.put("device" + i, device);  // 模拟多个设备信息
            }

            response.put("data", data);

            // 返回响应内容
            return response.toString();
        }
    }
}
