package com.amoo.executor;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

final class BrokerClient {
    static JSONObject call(Context context, String route, JSONObject body) throws Exception {
        File config = new File(context.getFilesDir(), "broker.json");
        if (!config.isFile()) throw new IOException("本机工具尚未配对，请重新运行电脑端安装器");
        JSONObject cfg = new JSONObject(new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8));
        int port = cfg.getInt("port");
        if (port != 18819) throw new IOException("Invalid local service port");
        HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + route).openConnection(Proxy.NO_PROXY);
        conn.setConnectTimeout(2000); conn.setReadTimeout(22000); conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Authorization", "Bearer " + cfg.getString("token"));
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try {
            if (body != null) {
                conn.setRequestMethod("POST"); conn.setDoOutput(true);
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8); conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
            }
            int status = conn.getResponseCode();
            InputStream stream = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            if (stream == null) throw new IOException("Local service returned HTTP " + status);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (InputStream in = stream) {
                byte[] buf = new byte[4096]; int size;
                while ((size = in.read(buf)) != -1) {
                    if (result.size() + size > 65536) throw new IOException("Local response too large");
                    result.write(buf, 0, size);
                }
            }
            JSONObject value = new JSONObject(result.toString("UTF-8"));
            if (status >= 400) value.put("ok", false);
            return value;
        } catch (ConnectException e) { throw new IOException("手机本机执行服务未启动，可能需在重启后恢复临时 Root 并启动服务"); }
        finally { conn.disconnect(); }
    }
}
