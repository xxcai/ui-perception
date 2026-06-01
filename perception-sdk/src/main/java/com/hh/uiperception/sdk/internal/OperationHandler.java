package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public final class OperationHandler {

    private static final String TAG = "PerceptionSdk";
    private static final int SCREEN_MARGIN = 50;

    private OperationHandler() {}

    public static OperationResponse fromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            if ("success".equals(obj.getString("status"))) {
                String ref = obj.getJSONObject("result").optString("ref", null);
                return OperationResponse.success(ref);
            } else {
                return OperationResponse.error(obj.getString("error"));
            }
        } catch (JSONException e) {
            return OperationResponse.error("Parse error");
        }
    }

    public static String handleClick(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String ref = json.getString("ref");

            int[] bounds = RefBoundsCache.getBounds(ref);
            if (bounds == null) {
                return OperationResponse.error("Unknown ref: " + ref + ". Capture UI first.").toJson();
            }

            float x = (bounds[0] + bounds[2]) / 2f;
            float y = (bounds[1] + bounds[3]) / 2f;

            Activity activity = ForegroundActivityTracker.getForegroundActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return OperationResponse.error("No foreground Activity").toJson();
            }

            boolean dispatched = TouchHandler.click(activity, x, y);
            if (!dispatched) {
                return OperationResponse.error("Click dispatch failed").toJson();
            }

            return OperationResponse.success(ref).toJson();
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"ref\":\"n1\"}").toJson();
        }
    }

    public static String handleSwipe(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String direction = json.getString("direction");
            String ref = json.optString("ref", null);

            Activity activity = ForegroundActivityTracker.getForegroundActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return OperationResponse.error("No foreground Activity").toJson();
            }

            float startX, startY, endX, endY;

            if (ref != null) {
                int[] bounds = RefBoundsCache.getBounds(ref);
                if (bounds == null) {
                    return OperationResponse.error("Unknown ref: " + ref).toJson();
                }
                float cx = (bounds[0] + bounds[2]) / 2f;
                float cy = (bounds[1] + bounds[3]) / 2f;
                float margin = Math.min(bounds[2] - bounds[0], bounds[3] - bounds[1]) * 0.2f;
                float[] coords = computeSwipeCoords(direction, cx, cy, margin);
                startX = coords[0]; startY = coords[1]; endX = coords[2]; endY = coords[3];
            } else {
                // Full screen swipe using screen center
                float screenCx = activity.getWindow().getDecorView().getWidth() / 2f;
                float screenCy = activity.getWindow().getDecorView().getHeight() / 2f;
                float[] coords = computeSwipeCoords(direction, screenCx, screenCy, SCREEN_MARGIN);
                startX = coords[0]; startY = coords[1]; endX = coords[2]; endY = coords[3];
            }

            boolean dispatched = TouchHandler.swipe(activity, startX, startY, endX, endY);
            if (!dispatched) {
                return OperationResponse.error("Swipe dispatch failed").toJson();
            }

            return OperationResponse.success(ref != null ? ref : "screen").toJson();
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"direction\":\"up\",\"ref\":\"n1\"}").toJson();
        }
    }

    private static float[] computeSwipeCoords(String direction, float cx, float cy, float margin) {
        // Swipe distance: ~40% of screen from center toward the edge
        float distance = 400f;
        float startX = cx, startY = cy, endX = cx, endY = cy;

        switch (direction) {
            case "up":
                startY = cy + distance / 2;
                endY = cy - distance / 2;
                break;
            case "down":
                startY = cy - distance / 2;
                endY = cy + distance / 2;
                break;
            case "left":
                startX = cx + distance / 2;
                endX = cx - distance / 2;
                break;
            case "right":
                startX = cx - distance / 2;
                endX = cx + distance / 2;
                break;
        }
        return new float[]{startX, startY, endX, endY};
    }
}
