package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.util.Log;

import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionComposer;
import com.hh.uiperception.core.PerceptionEntryResult;
import com.hh.uiperception.core.PerceptionPlan;
import com.hh.uiperception.core.PerceptionPlugin;
import com.hh.uiperception.core.PerceptionRunResult;
import com.hh.uiperception.core.PluginRegistry;

import java.util.List;

/**
 * 测试接口：返回所有插件的原始 capture 结果（不经过 transform）。
 */
public final class RawCaptureHandler {

    private static final String TAG = "PerceptionSdk";

    public static String capture() {
        Activity activity = ForegroundActivityTracker.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return "{\"status\":\"error\",\"error\":\"No foreground Activity\"}";
        }

        try {
            List<PerceptionPlugin> plugins = PluginRegistry.getApplicable(activity);
            if (plugins.isEmpty()) {
                return "{\"status\":\"error\",\"error\":\"No applicable plugin\"}";
            }

            PerceptionPlan plan = new PerceptionPlan(
                    activity.getClass().getSimpleName(),
                    plugins,
                    false
            );
            PerceptionRunResult runResult = PerceptionComposer.execute(activity, plan);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"status\":\"success\",\"result\":{");
            sb.append("\"activity\":\"").append(escape(activity.getClass().getSimpleName())).append("\",");
            sb.append("\"plugins\":[");

            boolean first = true;
            for (PerceptionEntryResult entry : runResult.entries()) {
                CaptureResult capture = entry.captureResult();
                if (capture == null || !capture.isSuccess()) continue;

                if (!first) sb.append(",");
                first = false;

                sb.append("{\"plugin\":\"").append(escape(entry.pluginName())).append("\"");
                sb.append(",\"contentType\":\"").append(escape(capture.contentType())).append("\"");
                sb.append(",\"content\":");
                if ("application/json".equals(capture.contentType())) {
                    sb.append(capture.content());
                } else {
                    sb.append("\"").append(escape(capture.content())).append("\"");
                }
                sb.append("}");
            }

            sb.append("]}}");
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Raw capture failed", e);
            return "{\"status\":\"error\",\"error\":\"" + escape(e.getMessage()) + "\"}";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
