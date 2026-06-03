package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.hh.uiperception.webplugin.WebDebugScript;
import com.hh.uiperception.webplugin.WebViewFinder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 调试接口：注入诊断脚本到 WebView，抓取原始 DOM 结构信息。
 * 用于排查 WebDomSerializer 遗漏节点的问题，不修改已有序列化逻辑。
 */
public final class DebugDomHandler {

    private static final String TAG = "PerceptionSdk";

    public static String capture() {
        Activity activity = ForegroundActivityTracker.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return "{\"status\":\"error\",\"error\":\"No foreground Activity\"}";
        }

        WebViewFinder.Finding finding = WebViewFinder.find(activity);
        if (!finding.isWeb || finding.dominantWebView == null) {
            return "{\"status\":\"error\",\"error\":\"No dominant WebView found\"}";
        }

        WebView webView = finding.dominantWebView;
        String js = WebDebugScript.script();

        AtomicReference<String> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            webView.evaluateJavascript(js, value -> {
                if (value != null) {
                    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                        value = value.replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\n", "\n")
                                .replace("\\t", "\t");
                    }
                    resultRef.set(value);
                }
                latch.countDown();
            });
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                return "{\"status\":\"error\",\"error\":\"JS evaluation timed out\"}";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "{\"status\":\"error\",\"error\":\"Interrupted\"}";
        }

        String result = resultRef.get();
        if (result == null || result.isEmpty()) {
            return "{\"status\":\"error\",\"error\":\"JS returned empty result\"}";
        }

        return "{\"status\":\"success\",\"result\":" + result + "}";
    }
}
