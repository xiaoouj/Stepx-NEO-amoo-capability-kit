package com.amoo.executor;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AmooAccessibilityService extends AccessibilityService {
    private static volatile AmooAccessibilityService instance;
    private final Handler main = new Handler(Looper.getMainLooper());

    static AmooAccessibilityService get() { return instance; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    JSONObject screenshot() throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("截屏不能在主线程等待");
        CountDownLatch latch = new CountDownLatch(1);
        String[] failure = new String[1];
        JSONObject[] result = new JSONObject[1];
        callOnMain(() -> {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult shot) {
                    try { result[0] = saveScreenshot(shot); }
                    catch (Exception e) { failure[0] = message(e); }
                    finally { latch.countDown(); }
                }
                @Override public void onFailure(int errorCode) {
                    failure[0] = screenshotError(errorCode);
                    latch.countDown();
                }
            });
            return null;
        });
        if (!latch.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("截屏超时");
        if (failure[0] != null) throw new IllegalStateException(failure[0]);
        if (result[0] == null) throw new IllegalStateException("截屏失败");
        return result[0];
    }

    private JSONObject saveScreenshot(ScreenshotResult shot) throws Exception {
        HardwareBuffer buffer = shot.getHardwareBuffer();
        Bitmap software;
        try {
            Bitmap hardware = Bitmap.wrapHardwareBuffer(buffer, shot.getColorSpace());
            if (hardware == null) throw new IllegalStateException("无法读取截屏缓冲区");
            software = hardware.copy(Bitmap.Config.ARGB_8888, false);
            hardware.recycle();
            if (software == null) throw new IllegalStateException("无法复制截屏");
        } finally {
            buffer.close();
        }
        int width = software.getWidth(), height = software.getHeight();
        byte[] png;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!software.compress(Bitmap.CompressFormat.PNG, 100, bytes)) throw new IllegalStateException("PNG编码失败");
            png = bytes.toByteArray();
        } finally {
            software.recycle();
        }
        String name = "amoo-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date()) + ".png";
        Uri uri = null;
        String path;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AmooTools");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("媒体库拒绝写入");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IllegalStateException("无法打开截屏输出流");
                out.write(png);
            }
            ContentValues done = new ContentValues(); done.put(MediaStore.Images.Media.IS_PENDING, 0);
            getContentResolver().update(uri, done, null, null);
            path = new File(Environment.getExternalStorageDirectory(), "Pictures/AmooTools/" + name).getAbsolutePath();
        } catch (Exception e) {
            if (uri != null) { try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) { } }
            File base = getExternalFilesDir(null);
            if (base == null) base = getFilesDir();
            File dir = new File(base, "screenshots");
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("截屏保存失败");
            File file = new File(dir, name);
            try (OutputStream out = new FileOutputStream(file)) { out.write(png); }
            uri = null;
            path = file.getAbsolutePath();
        }
        return new JSONObject().put("ok", true)
            .put("uri", uri == null ? JSONObject.NULL : uri.toString())
            .put("path", path).put("width", width).put("height", height).put("bytes", png.length)
            .put("mimeType", "image/png").put("timestamp", System.currentTimeMillis())
            .put("note", "图片保留在手机本机，未上传；path为手机路径");
    }

    private static String screenshotError(int code) {
        switch (code) {
            case ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT: return "截屏过于频繁，请稍后再试";
            case ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS: return "无障碍截屏权限不可用，请重新开启无障碍服务";
            case ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY: return "无效的显示设备";
            default: return "系统截屏失败（错误码 " + code + "）";
        }
    }

    JSONObject uiDump(int maxNodes) throws Exception {
        // 该机型会冻结后台进程；调用开始时先让主线程处理排队中的无障碍事件，避免返回上一次的节点快照。
        if (Looper.myLooper() != Looper.getMainLooper()) Thread.sleep(250);
        return callOnMain(() -> {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            JSONArray windows = new JSONArray();
            int[] count = {0};
            boolean[] truncated = {false};
            Set<Integer> seen = new HashSet<>();
            List<AccessibilityWindowInfo> ordered = new ArrayList<>(getWindows());
            ordered.sort((a, b) -> Integer.compare(windowRank(a), windowRank(b)));
            for (AccessibilityWindowInfo window : ordered) {
                if (window == null || !seen.add(window.getId())) continue;
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;
                root.refresh();
                JSONArray nodes = new JSONArray();
                addNode(root, nodes, 0, maxNodes, count, truncated);
                Rect bounds = new Rect(); window.getBoundsInScreen(bounds);
                windows.put(new JSONObject().put("id", window.getId()).put("type", window.getType())
                    .put("typeName", windowType(window.getType())).put("layer", window.getLayer())
                    .put("active", window.isActive()).put("focused", window.isFocused())
                    .put("bounds", bounds(bounds)).put("nodes", nodes));
            }
            if (windows.length() == 0) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    root.refresh();
                    JSONArray nodes = new JSONArray();
                    addNode(root, nodes, 0, maxNodes, count, truncated);
                    windows.put(new JSONObject().put("id", root.getWindowId()).put("type", 1)
                        .put("typeName", "application").put("active", true).put("focused", true)
                        .put("bounds", JSONObject.NULL).put("nodes", nodes));
                }
            }
            return new JSONObject().put("ok", true).put("nodeCount", count[0]).put("truncated", truncated[0])
                .put("maxNodes", maxNodes).put("screen", new JSONObject().put("width", dm.widthPixels)
                .put("height", dm.heightPixels).put("density", dm.density).put("densityDpi", dm.densityDpi))
                .put("windows", windows).put("note", "bounds为屏幕坐标[left,top,right,bottom]，可直接用于executor_tap");
        });
    }

    private static void addNode(AccessibilityNodeInfo node, JSONArray out, int depth, int maxNodes,
            int[] count, boolean[] truncated) throws Exception {
        if (node == null) return;
        if (count[0] >= maxNodes) { truncated[0] = true; return; }
        if (depth > 80) { truncated[0] = true; return; }
        count[0]++;
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        out.put(new JSONObject().put("depth", depth).put("text", text(node.getText()))
            .put("description", text(node.getContentDescription())).put("hint", text(node.getHintText()))
            .put("viewId", text(node.getViewIdResourceName())).put("className", text(node.getClassName()))
            .put("package", text(node.getPackageName())).put("bounds", bounds(bounds))
            .put("clickable", node.isClickable()).put("longClickable", node.isLongClickable())
            .put("editable", node.isEditable()).put("scrollable", node.isScrollable())
            .put("focusable", node.isFocusable()).put("focused", node.isFocused())
            .put("selected", node.isSelected()).put("enabled", node.isEnabled())
            .put("visible", node.isVisibleToUser()).put("checked", node.isChecked())
            .put("checkable", node.isCheckable()).put("childCount", node.getChildCount()));
        for (int i = 0; i < node.getChildCount(); i++) {
            if (count[0] >= maxNodes) { truncated[0] = true; break; }
            addNode(node.getChild(i), out, depth + 1, maxNodes, count, truncated);
        }
    }

    JSONObject tap(int x, int y) throws Exception {
        Path path = new Path(); path.moveTo(x, y);
        return gesture(path, 60, x, y, x, y);
    }

    JSONObject swipe(int x1, int y1, int x2, int y2, int durationMs) throws Exception {
        Path path = new Path(); path.moveTo(x1, y1); path.lineTo(x2, y2);
        return gesture(path, durationMs, x1, y1, x2, y2);
    }

    private JSONObject gesture(Path path, int durationMs, int x1, int y1, int x2, int y2) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("手势不能在主线程等待");
        CountDownLatch latch = new CountDownLatch(1);
        String[] failure = new String[1];
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs)).build();
        boolean dispatched = callOnMain(() -> dispatchGesture(gesture, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription description) { latch.countDown(); }
            @Override public void onCancelled(GestureDescription description) {
                failure[0] = "手势被系统取消";
                latch.countDown();
            }
        }, null));
        if (!dispatched) throw new IllegalStateException("手势注入失败，无障碍服务可能未连接");
        if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("手势执行超时");
        if (failure[0] != null) throw new IllegalStateException(failure[0]);
        return new JSONObject().put("ok", true).put("from", new JSONArray().put(x1).put(y1))
            .put("to", new JSONArray().put(x2).put(y2)).put("durationMs", durationMs);
    }

    JSONObject inputText(String value) throws Exception {
        JSONObject result = callOnMain(() -> {
            AccessibilityNodeInfo target = findEditable();
            if (target == null) throw new IllegalStateException("当前界面没有找到可编辑输入框，请先点击输入框再调用 executor_input_text");
            boolean alreadyFocused = target.isFocused();
            if (!alreadyFocused) target.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
            if (!target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                throw new IllegalStateException("输入框拒绝写入文字，请先点击输入框后重试");
            }
            return new JSONObject().put("ok", true).put("length", value.length())
                .put("viewId", text(target.getViewIdResourceName())).put("className", text(target.getClassName()))
                .put("package", text(target.getPackageName())).put("usedFallbackFocus", !alreadyFocused)
                .put("password", target.isPassword()).put("note", "输入内容未记录");
        });
        if (result.optBoolean("password")) {
            return result.put("verified", JSONObject.NULL).put("verificationSkipped", "password");
        }
        boolean verified = false;
        String observed = null;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(100);
            observed = callOnMain(() -> {
                AccessibilityNodeInfo node = findEditable();
                if (node == null) return null;
                node.refresh();
                CharSequence current = node.getText();
                if (current == null) return "";
                CharSequence hint = node.getHintText();
                if (hint != null && current.toString().contentEquals(hint)) return "";
                return current.toString();
            });
            if (observed != null && observed.equals(value)) { verified = true; break; }
        }
        result.put("verified", verified);
        if (!verified) result.put("observedLength", observed == null ? JSONObject.NULL : observed.length());
        return result;
    }

    private AccessibilityNodeInfo findEditable() {
        AccessibilityNodeInfo editable = editableFrom(findFocus(AccessibilityNodeInfo.FOCUS_INPUT));
        if (editable != null) return editable;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        return root == null ? null : firstEditable(root, 0);
    }

    private static AccessibilityNodeInfo editableFrom(AccessibilityNodeInfo node) {
        for (int i = 0; node != null && i < 4; i++) {
            if (node.isEditable() && node.isVisibleToUser()) return node;
            node = node.getParent();
        }
        return null;
    }

    private static AccessibilityNodeInfo firstEditable(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 60) return null;
        if (node.isEditable() && node.isVisibleToUser() && node.isEnabled()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = firstEditable(node.getChild(i), depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private <T> T callOnMain(Callable<T> callable) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) return callable.call();
        FutureTask<T> task = new FutureTask<>(callable);
        main.post(task);
        try {
            return task.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("无障碍服务响应超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("无障碍调用被中断");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw new IllegalStateException(String.valueOf(cause));
        }
    }

    private static JSONArray bounds(Rect r) throws Exception {
        return new JSONArray().put(r.left).put(r.top).put(r.right).put(r.bottom);
    }

    private static String text(CharSequence value) {
        if (value == null) return "";
        String text = value.toString();
        return text.length() > 300 ? text.substring(0, 300) : text;
    }

    private static String windowType(int type) {
        switch (type) {
            case AccessibilityWindowInfo.TYPE_APPLICATION: return "application";
            case AccessibilityWindowInfo.TYPE_INPUT_METHOD: return "input_method";
            case AccessibilityWindowInfo.TYPE_SYSTEM: return "system";
            case AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY: return "accessibility_overlay";
            case AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER: return "split_screen_divider";
            case AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY: return "magnification_overlay";
            default: return "unknown";
        }
    }

    private static int windowRank(AccessibilityWindowInfo window) {
        if (window == null) return 999;
        int rank = 0;
        if (window.isActive()) rank -= 100;
        if (window.isFocused()) rank -= 50;
        if (window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) rank -= 20;
        else if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) rank += 10;
        else if (window.getType() == AccessibilityWindowInfo.TYPE_SYSTEM) rank += 20;
        return rank;
    }

    private static String message(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
