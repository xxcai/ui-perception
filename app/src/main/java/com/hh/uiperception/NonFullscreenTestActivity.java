package com.hh.uiperception;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hh.uiperception.sdk.PerceptionSdk;
import com.hh.uiperception.sdk.internal.CaptureResponse;
import com.hh.uiperception.sdk.internal.OperationResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 验证 native 点击路径在"窗口在屏幕上有非零偏移"的情况下是否正确。
 *
 * 用 Dialog 主题让窗口悬浮居中（decorView 屏幕 Y != 0），等价于
 * "窗口被系统摆在状态栏下方"或"多窗口模式"等场景。默认主题（fullscreen=true
 * extra）切回 AppTheme，decorView 在 (0, 0)，用于回归对照。
 *
 * 触发方式：
 *   adb am start -n com.hh.uiperception/.NonFullscreenTestActivity --ez auto true
 *   adb am start -n com.hh.uiperception/.NonFullscreenTestActivity --ez auto true --ez fullscreen true
 *
 * 自动模式会等 UI settle 后跑测试，结果通过 logcat tag=NonFullscreenTest 输出。
 */
public class NonFullscreenTestActivity extends Activity {

    private static final String TAG = "NonFullscreenTest";
    private static final int BUTTON_COUNT = 5;
    private static final int TARGET_INDEX = 2;

    private final List<TextView> buttons = new ArrayList<>();
    private volatile int lastClickedIdx = -1;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 支持运行时切回普通 AppTheme（decorView 在 (0,0)），用于回归验证
        if (getIntent().getBooleanExtra("fullscreen", false)) {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView title = new TextView(this);
        title.setText("非全屏 Activity 偏移验证");
        title.setTextSize(22);
        title.setTextColor(0xFF202124);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Dialog 主题：窗口悬浮居中（decorView 屏幕 Y ≠ 0）。"
                + "目标：点击 Btn " + TARGET_INDEX + "，看实际落到哪个按钮。");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFF5F6368);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        content.addView(subtitle);

        for (int i = 0; i < BUTTON_COUNT; i++) {
            buttons.add(makeButton(i));
            content.addView(buttons.get(i));
        }

        statusView = new TextView(this);
        statusView.setText("等待测试");
        statusView.setTextSize(15);
        statusView.setTextColor(0xFF202124);
        statusView.setPadding(0, dp(8), 0, dp(8));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusLp.setMargins(0, dp(16), 0, 0);
        content.addView(statusView, statusLp);

        TextView runBtn = new TextView(this);
        runBtn.setText("▶ 运行 SDK 测试（capture + click ref of Btn " + TARGET_INDEX + "）");
        runBtn.setTextSize(15);
        runBtn.setGravity(Gravity.CENTER);
        runBtn.setTextColor(0xFFFFFFFF);
        runBtn.setBackgroundColor(0xFF1A73E8);
        runBtn.setMinHeight(dp(48));
        runBtn.setClickable(true);
        runBtn.setOnClickListener(v -> runTest());
        LinearLayout.LayoutParams runLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        runLp.setMargins(0, dp(8), 0, dp(8));
        content.addView(runBtn, runLp);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getColor(R.color.screen_background));
        scrollView.addView(content);
        setContentView(scrollView);

        if (getIntent().getBooleanExtra("auto", false)) {
            getWindow().getDecorView().postDelayed(this::runTest, 800);
        }
    }

    private TextView makeButton(int idx) {
        TextView btn = new TextView(this);
        btn.setText("Btn " + idx);
        btn.setTextSize(18);
        btn.setGravity(Gravity.CENTER);
        btn.setTextColor(0xFF1A73E8);
        btn.setBackgroundColor(0xFFE8F0FE);
        btn.setMinHeight(dp(64));
        btn.setClickable(true);
        btn.setOnClickListener(v -> onButtonClicked(idx, btn));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        btn.setLayoutParams(lp);
        return btn;
    }

    private void onButtonClicked(int idx, TextView btn) {
        lastClickedIdx = idx;
        for (TextView b : buttons) {
            b.setBackgroundColor(0xFFE8F0FE);
            b.setTextColor(0xFF1A73E8);
        }
        btn.setBackgroundColor(0xFF34A853);
        btn.setTextColor(0xFFFFFFFF);
        statusView.setText("手动点击: Btn " + idx);
        Log.i(TAG, ">>> Button clicked: Btn " + idx);
    }

    private void runTest() {
        statusView.setText("运行中...");
        // 后台线程跑测试 —— 这样 click() 返回后 UI 线程能继续处理 mPerformClick。
        // View.onTouchEvent 在 ACTION_UP 时 post(mPerformClick) 到 UI 队列；
        // 如果在 UI 线程同步等，会死锁。
        new Thread(this::runTestBackground).start();
    }

    private void runTestBackground() {
        // —— 诊断 + capture（UI 线程）——
        final CaptureResponse[] respArr = new CaptureResponse[1];
        final int[] decorLoc = new int[2];
        final int[][] btnLocs = new int[buttons.size()][2];
        final int[] btnHeights = new int[buttons.size()];
        CountDownLatch capLatch = new CountDownLatch(1);
        runOnUiThread(() -> {
            getWindow().getDecorView().getLocationOnScreen(decorLoc);
            for (int i = 0; i < buttons.size(); i++) {
                btnLocs[i] = new int[2];
                buttons.get(i).getLocationOnScreen(btnLocs[i]);
                btnHeights[i] = buttons.get(i).getHeight();
            }
            respArr[0] = PerceptionSdk.capture();
            capLatch.countDown();
        });
        try { capLatch.await(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        Log.i(TAG, "=== NonFullscreen click test ===");
        Log.i(TAG, "decorView screen pos: " + Arrays.toString(decorLoc));
        for (int i = 0; i < buttons.size(); i++) {
            Log.i(TAG, "Btn " + i + " screen Y range: [" + btnLocs[i][1] + ", "
                    + (btnLocs[i][1] + btnHeights[i]) + "]");
        }
        int decorY = decorLoc[1];

        CaptureResponse resp = respArr[0];
        if (resp == null || !resp.isSuccess()) {
            String err = resp == null ? "capture timeout" : resp.getError();
            Log.e(TAG, "Capture failed: " + err);
            runOnUiThread(() -> statusView.setText("Capture failed: " + err));
            return;
        }
        String yaml = resp.getYaml();
        Log.i(TAG, "YAML:\n" + yaml);

        // —— 找 Btn <TARGET> 的 ref ——
        String targetText = "Btn " + TARGET_INDEX;
        String ref = findRefByText(yaml, targetText);
        if (ref == null) {
            Log.e(TAG, "Could not find ref for '" + targetText + "' in YAML");
            runOnUiThread(() -> statusView.setText("找不到 ref for " + targetText));
            return;
        }
        Log.i(TAG, "Targeting '" + targetText + "' (ref=" + ref + ")");

        // —— reset state ——
        runOnUiThread(() -> {
            lastClickedIdx = -1;
            for (TextView b : buttons) {
                b.setBackgroundColor(0xFFE8F0FE);
                b.setTextColor(0xFF1A73E8);
            }
        });
        // 等 reset 跑完再 click
        SystemClock.sleep(100);

        // —— click ——（TouchHandler.click 内部用 runOnUiThread+CountDownLatch，可跨线程）
        OperationResponse clickResp = PerceptionSdk.click(ref);
        Log.i(TAG, "Click response: " + clickResp.toJson());

        // 后台线程 sleep，UI 线程自由处理 mPerformClick
        SystemClock.sleep(500);

        // —— 判定 ——
        int actual = lastClickedIdx;
        Log.i(TAG, ">>> After click: lastClickedIdx=" + actual + " (expected " + TARGET_INDEX + ")");
        if (decorY != 0) {
            Log.i(TAG, ">>> decorView Y offset = " + decorY + "px (非零 → 旧 Path A 会偏移)");
        }
        final String verdict;
        if (actual == TARGET_INDEX) {
            verdict = "✓ PASS: 正确点击 Btn " + TARGET_INDEX;
        } else if (actual == -1) {
            verdict = "✗ FAIL: 没有任何按钮被点击";
        } else {
            int offset = actual - TARGET_INDEX;
            verdict = "✗ FAIL: 点击了 Btn " + actual + "，预期 Btn " + TARGET_INDEX
                    + "（向下偏移 " + offset + " 个按钮，decorView Y=" + decorY + "px）";
        }
        Log.i(TAG, ">>> " + verdict);
        runOnUiThread(() -> statusView.setText(verdict));
    }

    private String findRefByText(String yaml, String text) {
        // 优先同一行：- <role> "Btn 2" [states...] [ref=nX] [bounds=...]
        Pattern sameLine = Pattern.compile("\"" + Pattern.quote(text) + "\".*\\[ref=(n\\d+|w\\d+)]");
        for (String line : yaml.split("\n")) {
            Matcher m = sameLine.matcher(line);
            if (m.find()) return m.group(1);
        }
        // 退回：找包含该文本的行，往祖先方向找最近一行带 ref
        String[] lines = yaml.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("\"" + text + "\"")) {
                for (int j = i; j >= Math.max(0, i - 6); j--) {
                    Matcher m = Pattern.compile("\\[ref=(n\\d+|w\\d+)]").matcher(lines[j]);
                    if (m.find()) return m.group(1);
                }
            }
        }
        return null;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
