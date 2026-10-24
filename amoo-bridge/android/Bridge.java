package com.amoo.bridge;

import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** ADB shell helper. Reads and operates the visible Amoo UI; no cloud credentials. */
public final class Bridge {
    private static final String PKG = "com.stepos.ailauncher";
    private static UiAutomation ui;
    private static Context context;
    private static String token;
    private static boolean running = true;
    private static final LinkedHashMap<String, JSONObject> sent = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || args[1].length() < 24) throw new IllegalArgumentException("port token");
        int port = Integer.parseInt(args[0]); token = args[1];
        Looper.prepareMainLooper();
        Class<?> at = Class.forName("android.app.ActivityThread");
        Object thread = at.getMethod("systemMain").invoke(null);
        context = (Context) at.getMethod("getSystemContext").invoke(thread);
        context = context.createPackageContext("com.android.shell", 0);
        HandlerThread events = new HandlerThread("amoo-ui-events"); events.start();
        Class<?> connType = Class.forName("android.app.IUiAutomationConnection");
        Object conn = Class.forName("android.app.UiAutomationConnection").getConstructor().newInstance();
        java.lang.reflect.Constructor<?> ctor = UiAutomation.class.getDeclaredConstructor(Looper.class, connType);
        ctor.setAccessible(true);
        ui = (UiAutomation) ctor.newInstance(events.getLooper(), conn);
        UiAutomation.class.getMethod("connect", int.class).invoke(ui, 1);
        AccessibilityServiceInfo info = ui.getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        ui.setServiceInfo(info);
        try (ServerSocket server = new ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(30000);
            System.out.println("AMOO_BRIDGE_READY " + port);
            while (running) {
                try (Socket socket = server.accept()) { socket.setSoTimeout(10000); handle(socket); }
                catch (SocketTimeoutException idle) { break; }
                catch (Exception e) { System.err.println(e.getClass().getSimpleName() + ": " + e.getMessage()); }
            }
        }
        UiAutomation.class.getMethod("disconnect").invoke(ui);
        events.quitSafely(); System.exit(0);
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') bytes.write(c);
            if (bytes.size() > 8192) throw new IOException("Header too large");
        }
        return bytes.toString("UTF-8");
    }
    private static void handle(Socket socket) throws Exception {
        InputStream in = socket.getInputStream(); String[] first = line(in).split(" ");
        if (first.length < 2) return;
        int size = 0; String auth = "";
        for (int n = 0; n < 64; n++) {
            String h = line(in); if (h.isEmpty()) break;
            int k = h.indexOf(':'); if (k < 0) continue;
            if (h.substring(0,k).equalsIgnoreCase("Content-Length")) size = Integer.parseInt(h.substring(k+1).trim());
            if (h.substring(0,k).equalsIgnoreCase("Authorization")) auth = h.substring(k+1).trim();
        }
        if (!auth.equals("Bearer " + token)) { respond(socket, 401, new JSONObject().put("error", "Unauthorized")); return; }
        if (size < 0 || size > 65536) { respond(socket, 413, new JSONObject().put("error", "Body too large")); return; }
        byte[] body = new byte[size]; int offset = 0;
        while (offset < size) { int n = in.read(body, offset, size-offset); if (n < 0) throw new EOFException(); offset += n; }
        try {
            JSONObject request = size == 0 ? new JSONObject() : new JSONObject(new String(body, StandardCharsets.UTF_8));
            String route = first[1];
            if (first[0].equals("GET") && route.equals("/snapshot")) respond(socket, 200, snapshot());
            else if (first[0].equals("POST") && route.equals("/open")) { open(); respond(socket, 200, snapshot()); }
            else if (first[0].equals("POST") && route.equals("/shutdown")) {
                running = false; respond(socket, 200, new JSONObject().put("stopped", true));
            } else if (first[0].equals("POST") && route.equals("/history")) {
                showHistory(); int page = request.optInt("page", 0);
                if (page != 0) {
                    AccessibilityNodeInfo sc = scrollable(root());
                    if (sc != null) sc.performAction(page > 0 ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                    Thread.sleep(330);
                }
                JSONArray sessions = sessions(snapshot().getJSONArray("nodes"));
                hideHistory(); respond(socket, 200, snapshot().put("sessions", sessions));
            } else if (first[0].equals("POST") && route.equals("/select")) {
                String title = request.getString("title"); int occurrence = request.optInt("occurrence", 0);
                showHistory(); JSONArray nodes = snapshot().getJSONArray("nodes");
                JSONArray sessions = sessions(nodes); JSONObject match = null;
                for(int i=0;i<sessions.length();i++) {
                    JSONObject s=sessions.getJSONObject(i);
                    if(s.getString("title").equals(title)&&s.getInt("occurrence")==occurrence){match=s;break;}
                }
                if(match==null) { hideHistory(); throw new IOException("手机会话列表已变化，请刷新列表后再选择"); }
                AccessibilityNodeInfo historyRoot=find("rv_chat_history", "");
                ArrayList<AccessibilityNodeInfo> candidates=new ArrayList<>();
                matchingTitles(historyRoot,title,candidates);
                AccessibilityNodeInfo target=occurrence>=0&&occurrence<candidates.size()?candidates.get(occurrence):null;
                if(target==null || !title.contentEquals(target.getText()==null?"":target.getText()) || !click(target)) {
                    hideHistory(); throw new IOException("会话位置已变化，请刷新列表后再选择");
                }
                Thread.sleep(420); respond(socket, 200, snapshot());
            }
            else if (first[0].equals("POST") && route.equals("/click")) {
                AccessibilityNodeInfo target = find(request.optString("id"), request.optString("text"));
                if (target == null || !click(target)) throw new IOException("指定控件当前不可用，请刷新手机界面");
                Thread.sleep(220); respond(socket, 200, snapshot());
            } else if (first[0].equals("POST") && route.equals("/send")) respond(socket, 200, send(request));
            else if (first[0].equals("POST") && route.equals("/new")) {
                hideHistory();
                AccessibilityNodeInfo draft=find("et_input_bar", "");
                if(draft!=null && !isEmptyInput(draft)) throw new IOException("手机输入框已有草稿，请先处理草稿再新建聊天");
                Intent intent = new Intent(); intent.setClassName(PKG, PKG + ".chat.activity.ChatMainActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); intent.putExtra("chat_new_chat", true);
                context.startActivity(intent); Thread.sleep(450); respond(socket, 200, snapshot());
            } else if (first[0].equals("POST") && route.equals("/scroll")) {
                AccessibilityNodeInfo root = root(); AccessibilityNodeInfo scroll = scrollable(root);
                if (scroll == null) throw new IOException("当前页面没有可滚动列表");
                boolean ok = scroll.performAction(request.optBoolean("older", true) ? AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD : AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                Thread.sleep(200); respond(socket, 200, snapshot().put("scrolled", ok));
            } else if (first[0].equals("POST") && route.equals("/back")) {
                ui.performGlobalAction(1); Thread.sleep(200); respond(socket, 200, snapshot());
            } else respond(socket, 404, new JSONObject().put("error", "Unknown route"));
        } catch (Exception e) { respond(socket, 409, new JSONObject().put("error", e.getMessage() == null ? e.toString() : e.getMessage())); }
    }
    private static void respond(Socket socket, int status, JSONObject value) throws Exception {
        byte[] data = value.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        out.write(("HTTP/1.1 " + status + " Result\r\nContent-Type: application/json; charset=utf-8\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: " + data.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(data); out.flush();
    }
    private static AccessibilityNodeInfo root() throws Exception {
        ui.clearCache(); AccessibilityNodeInfo root = ui.getRootInActiveWindow();
        if (root == null) throw new IOException("手机没有可读取的活动窗口，请解锁手机并打开 Amoo");
        if (!PKG.contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) throw new IOException("请解锁手机并打开 Amoo；当前窗口不是 Amoo");
        return root;
    }
    private static JSONObject snapshot() throws Exception {
        AccessibilityNodeInfo root = root(); JSONArray nodes = new JSONArray(); walk(root, nodes, "0", 0);
        JSONObject result = new JSONObject().put("nodes", nodes).put("mode", "ui-bridge").put("package", PKG);
        try (Cursor c = context.getContentResolver().query(Uri.parse("content://"+PKG+".state.provider/chat_active"), null, null, null, null)) {
            if (c != null && c.moveToFirst()) for (String name : c.getColumnNames()) result.put(name, c.getString(c.getColumnIndexOrThrow(name)));
        } catch (Exception ignored) { }
        boolean history=false,busy=false; JSONArray messages=new JSONArray();
        Map<String,Integer> repeats=new HashMap<>();
        for(int i=0;i<nodes.length();i++) {
            JSONObject node=nodes.getJSONObject(i); String id=node.getString("id"), text=node.getString("text");
            if(!node.getBoolean("visible")) continue;
            if(id.endsWith("/et_slide_panel_search")) history=true;
            if(text.equals("正在思考中")||text.startsWith("正在处理中 ")
                    ||(id.endsWith("/tv_tool_group_title")&&text.startsWith("正在执行"))
                    ||id.endsWith("/btn_stop")||id.endsWith("/btn_stop_generation")||id.endsWith("/btn_stop_send")) busy=true;
            if(id.endsWith("/tvSentMessage")) addMessage(messages,repeats,"user",text);
            if(id.endsWith("/receive_root_layout")) {
                String prefix=node.getString("path")+"."; ArrayList<String> parts=new ArrayList<>();
                for(int j=i+1;j<nodes.length();j++) {
                    JSONObject child=nodes.getJSONObject(j); if(!child.getString("path").startsWith(prefix)) break;
                    String t=child.getString("text");
                    if(child.getBoolean("visible")&&!t.isEmpty()&&!child.getString("class").contains("EditText")) parts.add(t);
                }
                if(!parts.isEmpty())addMessage(messages,repeats,"assistant",String.join("\n",parts));
            }
        }
        result.put("screen",history?"history":"chat").put("busy",busy).put("messages",messages);
        result.put("contextKey",digest(messages.toString()));
        return result;
    }
    private static String digest(String text) throws Exception {
        byte[] value=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder s=new StringBuilder();for(byte b:value)s.append(String.format("%02x",b&255));return s.toString();
    }
    private static void addMessage(JSONArray array,Map<String,Integer> repeats,String role,String text) throws Exception {
        if(text.isEmpty())return;
        String base=role+":"+digest(text);int index=repeats.containsKey(base)?repeats.get(base):0;repeats.put(base,index+1);
        array.put(new JSONObject().put("id",base+":"+index).put("role",role).put("text",text));
    }
    private static JSONArray sessions(JSONArray nodes) throws Exception {
        JSONArray result=new JSONArray(); Map<String,Integer> counts=new HashMap<>();
        for(int i=0;i<nodes.length();i++) {
            JSONObject row=nodes.getJSONObject(i); if(!row.getString("id").endsWith("/ll_drawer_item_content")||!row.getBoolean("visible"))continue;
            String prefix=row.getString("path")+".";
            for(int j=i+1;j<nodes.length();j++) {
                JSONObject child=nodes.getJSONObject(j);if(!child.getString("path").startsWith(prefix))break;
                String title=child.getString("text");
                if(child.getString("id").equals("android:id/text1")&&!title.isEmpty()) {
                    int occurrence=counts.containsKey(title)?counts.get(title):0;counts.put(title,occurrence+1);
                    result.put(new JSONObject().put("id",digest(title)+":"+occurrence).put("title",title).put("occurrence",occurrence).put("path",child.getString("path")));break;
                }
            }
        }
        return result;
    }
    private static void showHistory() throws Exception {
        if(find("et_slide_panel_search", "")!=null)return;
        AccessibilityNodeInfo menu=find("iv_menu", "");
        if(menu==null||!click(menu))throw new IOException("请在手机打开 Amoo 完整聊天界面");
        Thread.sleep(350);
        if(find("et_slide_panel_search", "")==null)throw new IOException("未能打开手机会话列表");
    }
    private static void hideHistory() throws Exception {
        if(find("et_slide_panel_search", "")!=null) { ui.performGlobalAction(1);Thread.sleep(300); }
    }
    private static void matchingTitles(AccessibilityNodeInfo node,String title,List<AccessibilityNodeInfo> out) {
        if(node==null)return;
        if("android:id/text1".equals(node.getViewIdResourceName())&&title.contentEquals(node.getText()==null?"":node.getText())&&node.isVisibleToUser())out.add(node);
        for(int i=0;i<node.getChildCount();i++)matchingTitles(node.getChild(i),title,out);
    }
    private static void walk(AccessibilityNodeInfo node, JSONArray nodes, String path, int depth) throws Exception {
        if (node == null || depth > 45 || nodes.length() >= 2200) return;
        Rect b = new Rect(); node.getBoundsInScreen(b);
        nodes.put(new JSONObject().put("path", path).put("id", str(node.getViewIdResourceName())).put("text", str(node.getText()))
            .put("description", str(node.getContentDescription())).put("class", str(node.getClassName()))
            .put("clickable", node.isClickable()).put("enabled", node.isEnabled()).put("editable", node.isEditable())
            .put("scrollable", node.isScrollable()).put("visible", node.isVisibleToUser())
            .put("bounds", new JSONArray(new int[]{b.left,b.top,b.right,b.bottom})));
        for (int i=0;i<node.getChildCount();i++) walk(node.getChild(i),nodes,path+"."+i,depth+1);
    }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
    private static AccessibilityNodeInfo find(String id, String text) throws Exception { return findIn(root(), id, text); }
    private static AccessibilityNodeInfo findIn(AccessibilityNodeInfo node, String id, String text) {
        if (node == null) return null;
        String actual = str(node.getViewIdResourceName());
        boolean matchesId = id.isEmpty() || actual.equals(id) || actual.equals(PKG+":id/"+id);
        if (matchesId && (text.isEmpty() || text.contentEquals(node.getText()==null?"":node.getText())) && node.isVisibleToUser() && node.isEnabled()) return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=findIn(node.getChild(i),id,text);if(found!=null)return found;}
        return null;
    }
    private static AccessibilityNodeInfo scrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable() && node.isVisibleToUser()) return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo n=scrollable(node.getChild(i));if(n!=null)return n;} return null;
    }
    private static boolean click(AccessibilityNodeInfo node) {
        for(int i=0;node!=null&&i<5;i++,node=node.getParent()) if(node.isClickable()) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return false;
    }
    private static void open() throws Exception {
        Intent intent = new Intent(); intent.setClassName(PKG, PKG+".chat.activity.ChatMainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); Thread.sleep(400);
    }
    private static JSONObject send(JSONObject request) throws Exception {
        String key=request.optString("requestId"), text=request.optString("text");
        if(key.length()<12 || text.trim().isEmpty() || text.length()>16000) throw new IOException("消息或请求标识无效");
        if(sent.containsKey(key)) return sent.get(key);
        JSONObject before=snapshot(); String expected=request.optString("sessionId");
        if(before.optString("screen").equals("history")) throw new IOException("请先关闭手机会话侧栏再发送");
        if(before.optBoolean("busy")) throw new IOException("Amoo 正在生成，请等它完成后再发送");
        String expectedContext=request.optString("contextKey");
        if(!expectedContext.isEmpty()&&!expectedContext.equals(before.optString("contextKey"))) throw new IOException("手机会话内容已变化，请查看最新内容后再发送");
        if(!expected.isEmpty() && !expected.equals(before.optString("session_id"))) throw new IOException("手机会话已切换，请刷新后再发送");
        AccessibilityNodeInfo input=find("et_input_bar", "");
        if(input==null) throw new IOException("Amoo 输入框不可见，请关闭手机上的菜单或弹窗");
        if(!isEmptyInput(input)) throw new IOException("手机输入框已有草稿，请先发送或清空草稿");
        Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
        if(!input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b)) throw new IOException("手机输入框不接受文本");
        Thread.sleep(160);
        AccessibilityNodeInfo button=find("btn_text_send", "");
        if(button==null) button=find("btn_send", "");
        if(button==null) button=find("iv_send", "");
        if(button==null) button=find("", "发送");
        if(button==null) throw new IOException("文字已填入手机，但发送按钮未找到；请在手机确认后发送");
        JSONObject accepted=new JSONObject().put("requestId",key).put("accepted",true).put("delivery","clicked");
        // Cache before clicking: an ambiguous network result must never cause a second click.
        sent.put(key,new JSONObject().put("requestId",key).put("accepted",false).put("delivery","unknown"));
        while(sent.size()>200)sent.remove(sent.keySet().iterator().next());
        if(!click(button)) throw new IOException("未能点击发送；请检查手机，不要自动重试");
        sent.put(key,accepted); return accepted;
    }
    private static boolean isEmptyInput(AccessibilityNodeInfo input) {
        String value=str(input.getText());return value.isEmpty()||value.equals("发消息或按住说话");
    }
}
