package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class WebViewExecutor {

    private static final String TAG = "PerceptionSdk";

    private WebViewExecutor() {}

    static String executeJs(Activity activity, String js) {
        WebView webView = findWebView(activity.getWindow().getDecorView());
        if (webView == null) {
            return OperationResponse.error("No WebView found").toJson();
        }

        String[] result = {null};
        CountDownLatch latch = new CountDownLatch(1);

        activity.runOnUiThread(() -> {
            webView.evaluateJavascript(js, value -> {
                result[0] = value;
                latch.countDown();
            });
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                return OperationResponse.error("JS execution timeout").toJson();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OperationResponse.error("JS execution interrupted").toJson();
        }

        // evaluateJavascript wraps result in quotes
        if (result[0] != null && result[0].startsWith("\"") && result[0].endsWith("\"")) {
            result[0] = result[0].substring(1, result[0].length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return result[0];
    }

    private static WebView findWebView(android.view.View view) {
        if (view instanceof WebView) {
            return (WebView) view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebView(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
