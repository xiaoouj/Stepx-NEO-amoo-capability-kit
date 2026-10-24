package com.amoo.settings;

import android.os.Process;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Small local privileged service. No arbitrary command, file, or Intent interface. */
public final class SettingsBroker {
    private static String token;
    private static Map<String, JSONObject> routes = new HashMap<>();
    private static boolean running = true;

    public static void main(String[] args) throws Exception {
        if (Process.myUid() != 0 || args.length != 1) throw new IOException("Root and config path required");
        JSONObject cfg = new JSONObject(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));
        token = cfg.getString("token");
        if (token.length() < 48) throw new IOException("Invalid token");
        JSONArray items = new JSONObject(new String(Files.readAllBytes(Paths.get(cfg.getString("routes"))), StandardCharsets.UTF_8)).getJSONArray("routes");
        for (int i = 0; i < items.length(); i++) {
            JSONObject r = items.getJSONObject(i); routes.put(r.getString("id"), r);
        }
        try (ServerSocket server = new ServerSocket(cfg.getInt("port"), 8, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("SETTINGS_BROKER_READY pid=" + Process.myPid());
            while (running) {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000); handle(socket);
                } catch (Exception e) { System.err.println(e.getClass().getSimpleName()); }
            }
        }
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int c; (c = in.read()) != -1;) {
            if (c == 10) break;
            if (c != 13) out.write(c);
            if (out.size() > 8192) throw new IOException("Header too large");
        }
        return out.toString("UTF-8");
    }

    private static void handle(Socket socket) throws Exception {
        InputStream in = socket.getInputStream();
        String[] first = line(in).split(" ");
        int length = 0; String auth = ""; boolean ended = false;
        for (int n = 0; n < 32; n++) {
            String h = line(in);
            if (h.isEmpty()) { ended = true; break; }
            int k = h.indexOf(':'); if (k < 0) continue;
            String name = h.substring(0, k);
            if (name.equalsIgnoreCase("Content-Length")) length = Integer.parseInt(h.substring(k+1).trim());
            if (name.equalsIgnoreCase("Authorization")) auth = h.substring(k+1).trim();
        }
        if (!ended || first.length < 2 || length < 0 || length > 4096) { reply(socket, 400, new JSONObject().put("error", "Invalid request")); return; }
        if (!MessageDigest.isEqual(auth.getBytes(StandardCharsets.UTF_8), ("Bearer " + token).getBytes(StandardCharsets.UTF_8))) {
            reply(socket, 401, new JSONObject().put("error", "Unauthorized")); return;
        }
        byte[] data = new byte[length]; int offset = 0;
        while (offset < length) { int n = in.read(data, offset, length-offset); if (n < 0) throw new EOFException(); offset += n; }
        try {
            JSONObject req = length == 0 ? new JSONObject() : new JSONObject(new String(data, StandardCharsets.UTF_8));
            JSONObject result;
            if (first[0].equals("GET") && first[1].equals("/health")) {
                result = new JSONObject().put("uid", Process.myUid()).put("pid", Process.myPid()).put("ok", true);
            } else if (first[0].equals("POST") && first[1].equals("/open")) {
                JSONObject route = routes.get(req.getString("id"));
                if (route == null) throw new IOException("Unknown route; search the index first");
                ArrayList<String> argv = new ArrayList<>(Arrays.asList("/system/bin/am", "start", "-W", "-n", route.getString("component")));
                JSONArray extra = route.getJSONArray("args");
                for (int i = 0; i < extra.length(); i++) argv.add(extra.getString(i));
                result = execute(argv, false);
                Thread.sleep(350);
                JSONObject focus = execute(Arrays.asList("/system/bin/dumpsys", "window"), true);
                result.put("id", route.getString("id")).put("title", route.getString("title"))
                    .put("focus", focus.optString("output")).put("pageContentVerified", false);
                System.out.println("SETTINGS_OPEN " + route.getString("id") + " ok=" + result.optBoolean("ok") + " time=" + System.currentTimeMillis());
            } else if (first[0].equals("POST") && first[1].equals("/navigate")) {
                String action = req.getString("action");
                String key;
                if (action.equals("back")) key = "4";
                else if (action.equals("home")) key = "3";
                else if (action.equals("recents")) key = "187";
                else throw new IOException("Unsupported navigation");
                result = execute(Arrays.asList("/system/bin/input", "keyevent", key), false).put("action", action);
                System.out.println("SETTINGS_NAVIGATE " + action + " ok=" + result.optBoolean("ok") + " time=" + System.currentTimeMillis());
            } else if (first[0].equals("POST") && first[1].equals("/shutdown")) {
                running = false; result = new JSONObject().put("ok", true);
            } else { reply(socket, 404, new JSONObject().put("error", "Unknown route")); return; }
            reply(socket, 200, result);
        } catch (Exception e) { reply(socket, 409, new JSONObject().put("error", String.valueOf(e.getMessage()))); }
    }

    private static JSONObject execute(List<String> argv, boolean focusOnly) throws Exception {
        java.lang.Process process = new ProcessBuilder(argv).redirectErrorStream(true).start();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        Future<String> output = reader.submit(() -> {
            StringBuilder text = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String row;
                while ((row = r.readLine()) != null) {
                    if (focusOnly && !row.contains("mCurrentFocus=") && !row.contains("mFocusedApp=")) continue;
                    if (text.length() < 24000) text.append(row).append('\n');
                }
            }
            return text.toString();
        });
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) throw new IOException("Command timed out");
            String text = output.get(2, TimeUnit.SECONDS);
            boolean ok = process.exitValue() == 0 && !text.contains("Error:") && !text.contains("SecurityException") && !text.contains("Exception occurred");
            return new JSONObject().put("ok", ok).put("exitCode", process.exitValue()).put("output", text);
        } finally { process.destroyForcibly(); reader.shutdownNow(); }
    }

    private static void reply(Socket socket, int status, JSONObject result) throws IOException {
        byte[] body = result.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 " + status + " Result\r\nContent-Type: application/json; charset=utf-8\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(body); out.flush();
    }
}
