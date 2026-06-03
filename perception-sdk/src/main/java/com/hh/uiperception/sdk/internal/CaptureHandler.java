package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.util.Log;

import com.hh.uiperception.core.CaptureRequest;
import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionPlugin;
import com.hh.uiperception.core.PluginRegistry;
import com.hh.uiperception.core.WebFusionMode;
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
            String yaml;
            SemanticNode nativeTree = trees.get("native");
            SemanticNode webTree = trees.get("web");

            if (nativeTree != null && webTree != null) {
                WebFusionMode mode = PerceptionSdk.fusionMode();
                if (mode == WebFusionMode.WEB_ONLY) {
                    yaml = SemanticFusion.fuse(null, webTree);
                } else {
                    yaml = SemanticFusion.fuse(nativeTree, webTree);
                }
            } else if (webTree != null) {
                yaml = SemanticFusion.fuse(null, webTree);
            } else {
                yaml = SemanticFusion.fuse(nativeTree, null);
            }

            String activityClassName = captures.values().iterator().next().activityClassName();
            RefBoundsCache.update(yaml);
            return CaptureResponse.success(activityClassName, yaml);
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
}
