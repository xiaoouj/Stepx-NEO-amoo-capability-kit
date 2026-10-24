package com.amoo.executor;

import android.app.appfunctions.*;
import android.app.appsearch.GenericDocument;
import android.content.pm.SigningInfo;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class PhoneToolsService extends AppFunctionService {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static final Set<String> CALLERS = new HashSet<>(Arrays.asList(
        "com.stepos.ability", "com.stepos.agentbus", "com.stepos.aiengine", "com.stepos.ailauncher", "com.stepos.stepclaw", "com.android.shell"));
    private static final String PREFIX = "com.amoo.executor.PhoneTools#";

    @Override public void onExecuteFunction(ExecuteAppFunctionRequest request, String callingPackage, SigningInfo signingInfo,
            CancellationSignal cancellation, OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException> callback) {
        if (!CALLERS.contains(callingPackage)) {
            callback.onError(new AppFunctionException(AppFunctionException.ERROR_DENIED, "Caller not enabled for these device tools")); return;
        }
        worker.execute(() -> {
            String name = request.getFunctionIdentifier();
            try {
                cancellation.throwIfCanceled();
                GenericDocument params = request.getParameters();
                JSONObject result;
                if (name.equals(PREFIX + "openSettings")) {
                    String page = required(params, "page");
                    String id = resolvePage(page);
                    result = BrokerClient.call(this, "/open", new JSONObject().put("id", id));
                } else if (name.equals(PREFIX + "navigate")) {
                    String action = required(params, "action");
                    if (!Arrays.asList("home", "back", "recents").contains(action)) throw new IllegalArgumentException("action must be home, back, or recents");
                    result = BrokerClient.call(this, "/navigate", new JSONObject().put("action", action));
                } else if (name.equals(PREFIX + "searchSettings")) {
                    result = search(required(params, "query"));
                } else if (name.equals(PREFIX + "getToolStatus")) {
                    result = status();
                } else if (name.equals(PREFIX + "screenshot")) {
                    result = accessibility().screenshot();
                } else if (name.equals(PREFIX + "uiDump")) {
                    result = accessibility().uiDump(optionalInt(params, "maxNodes", 200, 1, 1000));
                } else if (name.equals(PREFIX + "tap")) {
                    result = accessibility().tap(requiredInt(params, "x", 0, 20000), requiredInt(params, "y", 0, 20000));
                } else if (name.equals(PREFIX + "swipe")) {
                    result = accessibility().swipe(requiredInt(params, "x1", 0, 20000), requiredInt(params, "y1", 0, 20000),
                        requiredInt(params, "x2", 0, 20000), requiredInt(params, "y2", 0, 20000),
                        optionalInt(params, "durationMs", 300, 1, 5000));
                } else if (name.equals(PREFIX + "inputText")) {
                    result = accessibility().inputText(requiredText(params, "text", 2000));
                } else {
                    callback.onError(new AppFunctionException(AppFunctionException.ERROR_FUNCTION_NOT_FOUND, "Unknown local tool")); return;
                }
                cancellation.throwIfCanceled();
                Log.i("AmooPhoneTools", "function=" + name.substring(PREFIX.length()) + " caller=" + callingPackage + " ok=" + result.optBoolean("ok"));
                GenericDocument doc = new GenericDocument.Builder<>("", "", "")
                    .setPropertyString(ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE, result.toString()).build();
                callback.onResult(new ExecuteAppFunctionResponse(doc));
            } catch (IllegalArgumentException e) {
                callback.onError(new AppFunctionException(AppFunctionException.ERROR_INVALID_ARGUMENT, e.getMessage()));
            } catch (android.os.OperationCanceledException e) {
                callback.onError(new AppFunctionException(AppFunctionException.ERROR_CANCELLED, "Cancelled"));
            } catch (IllegalStateException e) {
                callback.onError(new AppFunctionException(AppFunctionException.ERROR_APP_UNKNOWN_ERROR,
                    e.getMessage() == null ? "本机工具当前不可用" : e.getMessage()));
            } catch (Exception e) {
                // Do not print exception payloads or the private pairing configuration.
                callback.onError(new AppFunctionException(AppFunctionException.ERROR_APP_UNKNOWN_ERROR,
                    e instanceof IOException ? e.getMessage() : "本机工具执行失败：" + e.getClass().getSimpleName()));
            }
        });
    }

    private static String raw(GenericDocument params, String name) {
        try {
            String value = params.getPropertyString(name);
            if (value != null) return value;
        } catch (IllegalArgumentException ignored) { }
        try {
            return Long.toString(params.getPropertyLong(name));
        } catch (IllegalArgumentException ignored) { }
        return null;
    }

    private static String required(GenericDocument params, String name) {
        String value = raw(params, name);
        if (value == null || value.trim().isEmpty() || value.length() > 128) throw new IllegalArgumentException("Invalid " + name);
        return value.trim();
    }

    private static String requiredText(GenericDocument params, String name, int max) {
        String value = raw(params, name);
        if (value == null || value.length() > max) throw new IllegalArgumentException(name + " must be a string of at most " + max + " chars");
        return value;
    }

    private static int requiredInt(GenericDocument params, String name, int min, int max) {
        String value = raw(params, name);
        if (value == null) throw new IllegalArgumentException("Invalid " + name);
        return parseRange(value, name, min, max);
    }

    private static int optionalInt(GenericDocument params, String name, int fallback, int min, int max) {
        String value = raw(params, name);
        if (value == null || value.trim().isEmpty()) return fallback;
        return parseRange(value, name, min, max);
    }

    private static int parseRange(String value, String name, int min, int max) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer in [" + min + "," + max + "]");
        }
    }

    private static AmooAccessibilityService accessibility() {
        AmooAccessibilityService service = AmooAccessibilityService.get();
        if (service == null) throw new IllegalStateException("无障碍服务未连接：请打开 AMOO 本机工具并开启无障碍服务");
        return service;
    }

    private String asset(String path) throws Exception {
        try (InputStream in = getAssets().open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    private String resolvePage(String page) throws Exception {
        JSONArray routes = new JSONObject(asset("routes.json")).getJSONArray("routes");
        for (int i = 0; i < routes.length(); i++) {
            JSONObject r = routes.getJSONObject(i);
            if (page.equals(r.getString("id")) || page.equals(r.getString("title"))) return r.getString("id");
            JSONArray aliases = r.getJSONArray("aliases");
            for (int j = 0; j < aliases.length(); j++) if (page.equals(aliases.getString(j))) return r.getString("id");
        }
        throw new IllegalArgumentException("尚未登记这个直达页面；可用：开发者选项、无线调试、输入法设置、电池健康度");
    }

    private JSONObject search(String query) throws Exception {
        JSONObject index = new JSONObject(asset("settings-index.json"));
        JSONArray rows = new JSONArray(); int total = 0;
        String q = query.toLowerCase(Locale.ROOT);
        for (String group : Arrays.asList("activities", "preferences")) {
            JSONArray items = index.getJSONArray(group);
            for (int i = 0; i < items.length(); i++) {
                JSONObject row = items.getJSONObject(i);
                if (row.toString().toLowerCase(Locale.ROOT).contains(q)) {
                    total++; if (rows.length() < 20) rows.put(row);
                }
            }
        }
        return new JSONObject().put("ok", true).put("matches", rows).put("total", total)
            .put("note", "静态索引候选不等于可直达页面；已登记直达：developer、wireless-debugging、keyboard、battery-health");
    }

    private JSONObject status() throws Exception {
        JSONObject service;
        boolean serviceOk = false;
        try {
            service = BrokerClient.call(this, "/health", null);
            serviceOk = service.optBoolean("ok");
        } catch (Exception e) {
            service = new JSONObject().put("ok", false)
                .put("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        AmooAccessibilityService accessibility = AmooAccessibilityService.get();
        boolean connected = accessibility != null;
        return new JSONObject().put("ok", serviceOk).put("service", service)
            .put("accessibility", new JSONObject().put("connected", connected)
                .put("screenshot", connected).put("uiDump", connected).put("tap", connected)
                .put("swipe", connected).put("inputText", connected))
            .put("note", "settings/navigate依赖本机执行服务；截屏/UI读取/点击/滑动/输入依赖无障碍服务");
    }

    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
