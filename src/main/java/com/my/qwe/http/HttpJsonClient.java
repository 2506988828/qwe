package com.my.qwe.http;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class HttpJsonClient {

    public static JSONObject post(String apiUrl, JSONObject jsonBody) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        byte[] data = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
        conn.getOutputStream().write(data);

        StringBuilder responseBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), "GBK")) {
            while (scanner.hasNextLine()) {
                responseBuilder.append(scanner.nextLine());
            }
        }

        return new JSONObject(responseBuilder.toString());
    }
}
