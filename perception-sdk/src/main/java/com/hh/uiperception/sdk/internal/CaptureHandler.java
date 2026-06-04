package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;
import android.view.View;
import android.view.ViewGroup;

import com.hh.uiperception.core.CaptureRequest;
import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionPlugin;
import com.hh.uiperception.core.PluginRegistry;
import com.hh.uiperception.core.WebFusionMode;
import com.hh.uiperception.core.semantic.FusionResult;
import com.hh.uiperception.core.semantic.SemanticFusion;
import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.sdk.PerceptionSdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CaptureHandler {

    private static final String TAG = "PerceptionSdk";

    public static CaptureResponse capture() {
        Activity activity = ForegroundActivityTracker.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return CaptureResponse.error("No foreground Activity");
        }

        try {
            List<PerceptionPlugin> plugins = PluginRegistry.getApplicable(activity);
            if (plugins.isEmpty()) {
                return CaptureResponse.error("No applicable plugin for " + activity.getClass().getSimpleName());
            }

            String baselineId = activity.getClass().getSimpleName();

            // Phase 1: Run captures
            Map<String, CaptureResult> captures = new LinkedHashMap<>();
            for (PerceptionPlugin plugin : plugins) {
                CaptureRequest request = new CaptureRequest(baselineId, plugin.name());
                CaptureResult result = plugin.capture(activity, request);
                if (result != null && result.isSuccess()) {
                    captures.put(plugin.name(), result);
                }
            }

            if (captures.isEmpty()) {
                return CaptureResponse.error("All captures failed");
            }

            // Phase 2: Build semantic trees
            Map<String, SemanticNode> trees = new LinkedHashMap<>();
            for (Map.Entry<String, CaptureResult> entry : captures.entrySet()) {
                PerceptionPlugin plugin = findPlugin(plugins, entry.getKey());
                if (plugin != null) {
                    SemanticNode tree = plugin.buildSemanticTree(entry.getValue());
                    if (tree != null) {
                        trees.put(entry.getKey(), tree);
                    }
                }
            }

            if (trees.isEmpty()) {
                return CaptureResponse.error("All semantic tree builds failed");
            }

            // Phase 3: Fusion or individual render
            SemanticNode nativeTree = trees.get("native");
            SemanticNode webTree = trees.get("web");

            FusionResult fusionResult;
            if (nativeTree != null && webTree != null) {
                WebFusionMode mode = PerceptionSdk.fusionMode();
                if (mode == WebFusionMode.WEB_ONLY) {
                    fusionResult = SemanticFusion.fuse(null, webTree);
                } else {
                    fusionResult = SemanticFusion.fuse(nativeTree, webTree);
                }
            } else if (webTree != null) {
                fusionResult = SemanticFusion.fuse(null, webTree);
            } else {
                fusionResult = SemanticFusion.fuse(nativeTree, null);
            }

            // Phase 4: Record WebView offset for coordinate-based operations
            int[] webViewOffset = findWebViewOffset(activity);

            String activityClassName = captures.values().iterator().next().activityClassName();
            RefBoundsCache.update(fusionResult.yaml(), fusionResult.webElementMap(), webViewOffset);
            return CaptureResponse.success(activityClassName, fusionResult.yaml());
        } catch (Exception e) {
            Log.e(TAG, "Capture failed", e);
            return CaptureResponse.error("Capture failed: " + e.getMessage());
        }
    }

    private static PerceptionPlugin findPlugin(List<PerceptionPlugin> plugins, String name) {
        for (PerceptionPlugin plugin : plugins) {
            if (plugin.name().equals(name)) {
                return plugin;
            }
        }
        return null;
    }

    private static int[] findWebViewOffset(Activity activity) {
        WebView webView = findWebView(activity.getWindow().getDecorView());
        if (webView != null) {
            int[] location = new int[2];
            webView.getLocationOnScreen(location);
            return new int[]{location[0], location[1]};
        }
        return new int[]{0, 0};
    }

    private static WebView findWebView(View view) {
        if (view instanceof WebView) {
            return (WebView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
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
