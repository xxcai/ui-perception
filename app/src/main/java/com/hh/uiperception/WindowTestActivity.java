package com.hh.uiperception;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 窗口捕获测试页：验证 SDK 对 4 种弹出场景的识别能力。
 * 1. DialogActivity（主题为 Dialog 的 Activity）
 * 2. AlertDialog
 * 3. PopupWindow
 * 4. 手动 WindowManager.addView()
 */
public class WindowTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("窗口捕获测试");
        title.setTextSize(24);
        title.setTextColor(0xFF202124);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("依次测试 Dialog / PopupWindow / 手动添加窗口 / DialogActivity 的捕获");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFF5F6368);
        subtitle.setPadding(0, dp(4), 0, dp(20));
        content.addView(subtitle);

        content.addView(testButton("1. 弹出 AlertDialog", this::showAlertDialog));
        content.addView(testButton("2. 弹出 PopupWindow", this::showPopupWindow));
        content.addView(testButton("3. 手动添加窗口 (WindowManager)", this::showManualWindow));
        content.addView(testButton("4. 打开 DialogActivity", this::showDialogActivity));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColor(R.color.screen_background));
        scrollView.addView(content);
        setContentView(scrollView);
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("AlertDialog 测试")
                .setMessage("这是一个 AlertDialog，验证能否被 SDK 抓取到。")
                .setPositiveButton("确定", null)
                .setNegativeButton("取消", null)
                .show();
    }

    private void showPopupWindow() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));
        layout.setBackgroundColor(Color.WHITE);

        TextView tv = new TextView(this);
        tv.setText("PopupWindow 测试内容");
        tv.setTextSize(18);
        tv.setTextColor(0xFF202124);
        layout.addView(tv);

        TextView desc = new TextView(this);
        desc.setText("验证 PopupWindow 能否被 SDK 抓取到");
        desc.setTextSize(14);
        desc.setTextColor(0xFF5F6368);
        desc.setPadding(0, dp(8), 0, dp(8));
        layout.addView(desc);

        Button dismiss = new Button(this);
        dismiss.setText("关闭");
        PopupWindow[] popupHolder = new PopupWindow[1];
        dismiss.setOnClickListener(v -> {
            if (popupHolder[0] != null) popupHolder[0].dismiss();
        });
        layout.addView(dismiss);

        PopupWindow popup = new PopupWindow(layout, dp(600), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(null);
        popup.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        popupHolder[0] = popup;
    }

    private void showManualWindow() {
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.WHITE);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView tv = new TextView(this);
        tv.setText("手动添加窗口测试");
        tv.setTextSize(18);
        tv.setTextColor(0xFF202124);
        inner.addView(tv);

        TextView desc = new TextView(this);
        desc.setText("通过 WindowManager.addView() 手动添加");
        desc.setTextSize(14);
        desc.setTextColor(0xFF5F6368);
        desc.setPadding(0, dp(8), 0, dp(8));
        inner.addView(desc);

        Button dismiss = new Button(this);
        dismiss.setText("关闭");
        dismiss.setOnClickListener(v -> getWindowManager().removeView(layout));
        inner.addView(dismiss);

        layout.addView(inner);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(600), ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;
        params.dimAmount = 0.5f;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindowManager().addView(layout, params);
    }

    private void showDialogActivity() {
        startActivity(new Intent(this, DialogStyleActivity.class));
    }

    private View testButton(String text, Runnable action) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(0xFF1A73E8);
        btn.setTextSize(15);
        btn.setGravity(Gravity.CENTER);
        btn.setMinHeight(dp(48));
        btn.setPadding(dp(12), dp(12), dp(12), dp(12));
        btn.setBackgroundColor(0xFFE8F0FE);
        btn.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        btn.setLayoutParams(lp);
        return btn;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
