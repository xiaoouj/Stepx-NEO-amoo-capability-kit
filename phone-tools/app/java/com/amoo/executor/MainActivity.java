package com.amoo.executor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setText("AMOO 本机工具 0.2.0");
        TextView body = new TextView(this);
        body.setTextSize(16);
        body.setPadding(0, pad / 2, 0, pad);
        body.setText("设置直达：开发者选项、无线调试、输入法设置、电池健康度\n\n"
            + "无 Root 界面工具：截屏、UI 节点读取、点击、滑动、文字输入。需要开启无障碍服务；截屏和输入内容不会上传或记录。\n\n"
            + "设置直达和导航还依赖已配对的本机执行服务，手机重启后如不可用需要恢复临时 Root 并启动服务。");
        EditText field = new EditText(this);
        field.setHint("输入测试框（executor_input_text 可写入这里）");
        field.setTextSize(16);
        field.setSingleLine(true);
        field.setPadding(0, pad / 4, 0, pad / 4);
        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, pad / 2, 0, pad / 2);
        Button button = new Button(this);
        button.setText("打开无障碍设置");
        button.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
            catch (Exception e) { status.setText("无法打开无障碍设置：" + e.getClass().getSimpleName()); }
        });
        layout.addView(title);
        layout.addView(body);
        layout.addView(field);
        layout.addView(status);
        layout.addView(button);
        setContentView(layout);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (status == null) return;
        if (AmooAccessibilityService.get() != null) {
            status.setText("无障碍服务：已连接");
            status.setTextColor(Color.rgb(0, 120, 60));
            return;
        }
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean listed = enabled != null && enabled.contains(getPackageName() + "/");
        status.setText(listed ? "无障碍服务：已启用，等待系统连接" : "无障碍服务：未启用（截屏、点击、滑动、输入不可用）");
        status.setTextColor(listed ? Color.rgb(180, 100, 0) : Color.rgb(180, 40, 40));
    }
}
