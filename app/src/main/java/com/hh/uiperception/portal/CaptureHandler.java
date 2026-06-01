package com.hh.uiperception.portal;

import android.app.Activity;
import android.util.Log;

import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionComposer;
import com.hh.uiperception.core.PerceptionEntryResult;
import com.hh.uiperception.core.PerceptionPlan;
import com.hh.uiperception.core.PerceptionRunResult;
import com.hh.uiperception.core.TransformResult;
import com.hh.uiperception.nativeplugin.NativePerceptionPlugin;
import com.hh.uiperception.App;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

final class CaptureHandler {

    private static final String TAG = "PerceptionPortal";

    static String capture() {
        Activity activity = App.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return errorJson("No foreground Activity");
        }

        try {
            PerceptionPlan plan = new PerceptionPlan(
                    activity.getClass().getSimpleName(),
                    Collections.singletonList(new NativePerceptionPlugin()),
                    true
            );
            PerceptionRunResult runResult = PerceptionComposer.execute(activity, plan);

            for (PerceptionEntryResult entry : runResult.entries()) {
                CaptureResult capture = entry.captureResult();
                if (capture == null || !capture.isSuccess()) continue;

                TransformResult transform = entry.transformResult();
                if (transform != null && transform.isSuccess()) {
                    return successJson(capture.activityClassName(), transform.content());
                }
            }
            return errorJson("Capture pipeline produced no result");
        } catch (Exception e) {
            Log.e(TAG, "Capture failed", e);
            return errorJson("Capture failed: " + e.getMessage());
        }
    }

    private static String successJson(String activity, String yaml) {
        try {
            JSONObject result = new JSONObject();
            result.put("activity", activity);
            result.put("yaml", yaml);
            result.put("timestamp", System.currentTimeMillis());

            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("result", result);
            return response.toString();
        } catch (JSONException e) {
            return errorJson("JSON serialization failed");
        }
    }

    private static String errorJson(String message) {
        try {
            JSONObject response = new JSONObject();
            response.put("status", "error");
            response.put("error", message);
            return response.toString();
        } catch (JSONException e) {
            return "{\"status\":\"error\",\"error\":\"JSON error\"}";
        }
    }
}
