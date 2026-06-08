package com.hh.uiperception;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 以 Dialog 主题启动的 Activity，用于测试窗口捕获。
 */
public class DialogStyleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView tv = new TextView(this);
        tv.setText("DialogActivity 测试");
        tv.setTextSize(20);
        tv.setTextColor(0xFF202124);
        layout.addView(tv);

        TextView desc = new TextView(this);
        desc.setText("这是一个以 Dialog 主题启动的 Activity，验证能否被 SDK 抓取到。");
        desc.setTextSize(14);
        desc.setTextColor(0xFF5F6368);
        desc.setPadding(0, dp(8), 0, dp(16));
        layout.addView(desc);

        Button close = new Button(this);
        close.setText("关闭");
        close.setOnClickListener(v -> finish());
        layout.addView(close);

        setContentView(layout);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
