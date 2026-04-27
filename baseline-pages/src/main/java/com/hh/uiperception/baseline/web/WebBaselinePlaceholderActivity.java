package com.hh.uiperception.baseline.web;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WebBaselinePlaceholderActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(0xFFF6F7FB);

        TextView title = new TextView(this);
        title.setText("Web 首页占位");
        title.setTextColor(0xFF202124);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText("Phase1 只保留 Web 基准页面入口，暂不实现 H5 内容、WebView 采集或 WebDom 抓取。");
        description.setTextColor(0xFF5F6368);
        description.setTextSize(16);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(12), 0, 0);

        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
