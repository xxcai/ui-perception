package com.hh.uiperception.webplugin;

import android.app.Activity;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;

/**
 * 在 Activity 的 View 树中查找占屏面积最大的 WebView。
 */
public final class WebViewFinder {

    private static final float COVERAGE_THRESHOLD = 0.5f;

    private WebViewFinder() {}

    public static Finding find(Activity activity) {
        if (activity == null) return Finding.empty();

        View decorView = activity.getWindow().getDecorView();
        if (!(decorView instanceof ViewGroup)) return Finding.empty();

        long screenArea = getScreenArea(activity);
        if (screenArea <= 0) return Finding.empty();

        List<WebView> allWebViews = new ArrayList<>();
        collectWebViews((ViewGroup) decorView, allWebViews);
        if (allWebViews.isEmpty()) return Finding.empty();

        WebView largest = null;
        long largestArea = 0;
        for (WebView wv : allWebViews) {
            long area = getVisibleArea(wv);
            if (area > largestArea) {
                largestArea = area;
                largest = wv;
            }
        }

        if (largest == null) return Finding.empty();

        float coverage = (float) largestArea / screenArea;
        return new Finding(largest, coverage, coverage > COVERAGE_THRESHOLD);
    }

    private static void collectWebViews(ViewGroup group, List<WebView> out) {
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child instanceof WebView) {
                out.add((WebView) child);
            } else if (child instanceof ViewGroup) {
                collectWebViews((ViewGroup) child, out);
            }
        }
    }

    private static long getVisibleArea(View view) {
        Rect visibleRect = new Rect();
        if (!view.getGlobalVisibleRect(visibleRect)) return 0;
        return (long) visibleRect.width() * visibleRect.height();
    }

    private static long getScreenArea(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return (long) dm.widthPixels * dm.heightPixels;
    }

    public static class Finding {
        public final WebView dominantWebView;
        public final float coverageRatio;
        public final boolean isWeb;

        private Finding(WebView webView, float ratio, boolean web) {
            dominantWebView = webView;
            coverageRatio = ratio;
            isWeb = web;
        }

        static Finding empty() {
            return new Finding(null, 0f, false);
        }
    }
}
