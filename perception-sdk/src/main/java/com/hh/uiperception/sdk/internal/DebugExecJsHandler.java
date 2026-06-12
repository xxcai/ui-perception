package com.hh.uiperception.sdk.internal;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

public final class DebugExecJsHandler {

    private DebugExecJsHandler() {}

    public static String execute(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String js = json.getString("js");

            Activity activity = ForegroundActivityTracker.getForegroundActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return OperationResponse.error("No foreground Activity").toJson();
            }

            String result = WebViewExecutor.executeJs(activity, js);
            if (result == null) {
                return OperationResponse.error("JS execution returned null").toJson();
            }
            return OperationResponse.success(result).toJson();
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"js\":\"...\"}").toJson();
        }
    }
}
