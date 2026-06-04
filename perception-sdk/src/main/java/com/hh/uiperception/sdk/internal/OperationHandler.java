package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public final class OperationHandler {

    private static final String TAG = "PerceptionSdk";
    private static final int SCREEN_MARGIN = 50;
    private static final int DEFAULT_LONG_PRESS_MS = 500;

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

    // ── Click ──────────────────────────────────────────────

    public static String handleClick(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String ref = json.getString("ref");

            int[] coords = getScreenCoordsOrError(ref);
            if (coords == null) return coordsError(ref);

            Activity activity = getValidActivity();
            if (activity == null) return noActivityError();

            boolean dispatched = TouchHandler.click(activity, coords[0], coords[1]);
            if (!dispatched) {
                return OperationResponse.error("Click dispatch failed").toJson();
            }

            return OperationResponse.success(ref).toJson();
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"ref\":\"n1\"}").toJson();
        }
    }

    // ── Long Press ─────────────────────────────────────────

    public static String handleLongPress(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String ref = json.getString("ref");
            int duration = json.optInt("duration", DEFAULT_LONG_PRESS_MS);

            int[] coords = getScreenCoordsOrError(ref);
            if (coords == null) return coordsError(ref);

            Activity activity = getValidActivity();
            if (activity == null) return noActivityError();

            boolean dispatched = TouchHandler.longPress(activity, coords[0], coords[1], duration);
            if (!dispatched) {
                return OperationResponse.error("Long press dispatch failed").toJson();
            }

            return OperationResponse.success(ref).toJson();
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"ref\":\"n1\",\"duration\":500}").toJson();
        }
    }

    // ── Swipe ──────────────────────────────────────────────

    public static String handleSwipe(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String direction = json.getString("direction");
            String ref = json.optString("ref", null);

            Activity activity = getValidActivity();
            if (activity == null) return noActivityError();

            float startX, startY, endX, endY;

            if (ref != null) {
                int[] coords = getScreenCoordsOrError(ref);
                if (coords == null) return coordsError(ref);
                float cx = coords[0];
                float cy = coords[1];
                int[] bounds = RefBoundsCache.getBounds(ref);
                float margin = Math.min(bounds[2] - bounds[0], bounds[3] - bounds[1]) * 0.2f;
                float[] swipeCoords = computeSwipeCoords(direction, cx, cy, margin);
                startX = swipeCoords[0]; startY = swipeCoords[1];
                endX = swipeCoords[2]; endY = swipeCoords[3];
            } else {
                float screenCx = activity.getWindow().getDecorView().getWidth() / 2f;
                float screenCy = activity.getWindow().getDecorView().getHeight() / 2f;
                float[] swipeCoords = computeSwipeCoords(direction, screenCx, screenCy, SCREEN_MARGIN);
                startX = swipeCoords[0]; startY = swipeCoords[1];
                endX = swipeCoords[2]; endY = swipeCoords[3];
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

    // ── Type Text ──────────────────────────────────────────

    public static String handleTypeText(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String ref = json.getString("ref");
            String text = json.getString("text");
            boolean clear = json.optBoolean("clear", true);

            if (RefBoundsCache.isWebRef(ref)) {
                return handleWebTypeText(ref, text, clear);
            }
            return handleNativeTypeText(ref, text, clear);
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"ref\":\"w1\",\"text\":\"hello\",\"clear\":true}").toJson();
        }
    }

    private static String handleWebTypeText(String ref, String text, boolean clear) {
        int prIdx = RefBoundsCache.getWebElementIdx(ref);
        if (prIdx < 0) {
            return OperationResponse.error("No web element index for ref: " + ref).toJson();
        }

        Activity activity = getValidActivity();
        if (activity == null) return noActivityError();

        String jsResult = WebViewExecutor.executeJs(activity, WebActionScript.typeText(prIdx, text, clear));
        if (jsResult == null) {
            return OperationResponse.error("JS execution returned null").toJson();
        }
        return jsResult;
    }

    private static String handleNativeTypeText(String ref, String text, boolean clear) {
        int[] coords = getScreenCoordsOrError(ref);
        if (coords == null) return coordsError(ref);

        Activity activity = getValidActivity();
        if (activity == null) return noActivityError();

        // Click to focus
        TouchHandler.click(activity, coords[0], coords[1]);
        SystemClock.sleep(100);

        // Clear existing text by selecting all and deleting
        if (clear) {
            // Select all then delete
            activity.runOnUiThread(() -> {
                android.view.KeyEvent selectAll = new android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_A);
                activity.dispatchTouchEvent(android.view.MotionEvent.obtain(
                        android.os.SystemClock.uptimeMillis(),
                        android.os.SystemClock.uptimeMillis(),
                        android.view.MotionEvent.ACTION_DOWN,
                        coords[0], coords[1], 0));
            });
        }

        // Type each character via dispatchKeyEvent
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int keyCode = charToKeyCode(c);
            if (keyCode > 0) {
                int metaState = Character.isUpperCase(c) ? android.view.KeyEvent.META_SHIFT_ON : 0;
                final int kc = keyCode;
                final int ms = metaState;
                activity.runOnUiThread(() -> {
                    long time = android.os.SystemClock.uptimeMillis();
                    activity.dispatchKeyEvent(new android.view.KeyEvent(
                            time, time, android.view.KeyEvent.ACTION_DOWN, kc, 0, ms));
                    activity.dispatchKeyEvent(new android.view.KeyEvent(
                            time, time + 1, android.view.KeyEvent.ACTION_UP, kc, 0, ms));
                });
            }
        }

        return OperationResponse.success(ref).toJson();
    }

    // ── Check / Uncheck ────────────────────────────────────
    // Playwright strategy: click + verify state change

    public static String handleCheck(String body) {
        return handleSetChecked(body, true);
    }

    public static String handleUncheck(String body) {
        return handleSetChecked(body, false);
    }

    private static String handleSetChecked(String body, boolean desiredState) {
        try {
            JSONObject json = new JSONObject(body);
            String ref = json.getString("ref");

            Activity activity = getValidActivity();
            if (activity == null) return noActivityError();

            if (RefBoundsCache.isWebRef(ref)) {
                // Check current state via JS
                int prIdx = RefBoundsCache.getWebElementIdx(ref);
                if (prIdx < 0) {
                    return OperationResponse.error("No web element index for ref: " + ref).toJson();
                }
                String stateResult = WebViewExecutor.executeJs(activity, WebActionScript.getCheckedState(prIdx));
                if (stateResult != null) {
                    try {
                        JSONObject stateJson = new JSONObject(stateResult);
                        if ("success".equals(stateJson.optString("status"))) {
                            JSONObject result = stateJson.getJSONObject("result");
                            boolean currentChecked = result.optBoolean("checked", false);
                            String ariaChecked = result.optString("ariaChecked", null);
                            boolean isChecked = currentChecked || "true".equals(ariaChecked);
                            if (isChecked == desiredState) {
                                return OperationResponse.success(ref).toJson();
                            }
                        }
                    } catch (JSONException ignored) {}
                }

                // Click to toggle
                int[] coords = getScreenCoordsOrError(ref);
                if (coords == null) return coordsError(ref);
                TouchHandler.click(activity, coords[0], coords[1]);

                // Verify state changed
                SystemClock.sleep(200);
                String afterResult = WebViewExecutor.executeJs(activity, WebActionScript.getCheckedState(prIdx));
                if (afterResult != null) {
                    try {
                        JSONObject afterJson = new JSONObject(afterResult);
                        if ("success".equals(afterJson.optString("status"))) {
                            JSONObject result = afterJson.getJSONObject("result");
                            boolean currentChecked = result.optBoolean("checked", false);
                            String ariaChecked = result.optString("ariaChecked", null);
                            boolean isChecked = currentChecked || "true".equals(ariaChecked);
                            if (isChecked == desiredState) {
                                return OperationResponse.success(ref).toJson();
                            }
                        }
                    } catch (JSONException ignored) {}
                }
                return OperationResponse.error("Check state did not change as expected").toJson();
            }

            // Native: just click (checkbox toggle)
            int[] coords = getScreenCoordsOrError(ref);
            if (coords == null) return coordsError(ref);
            boolean dispatched = TouchHandler.click(activity, coords[0], coords[1]);
            if (!dispatched) {
                return OperationResponse.error("Click dispatch failed").toJson();
            }
            return OperationResponse.success(ref).toJson();
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"ref\":\"n1\"}").toJson();
        }
    }

    // ── Select Option ──────────────────────────────────────
    // Playwright strategy: pure JS — option.selected = true + events

    public static String handleSelectOption(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String ref = json.getString("ref");
            String value = json.getString("value");

            if (!RefBoundsCache.isWebRef(ref)) {
                return OperationResponse.error("select_option only supported for web elements").toJson();
            }

            int prIdx = RefBoundsCache.getWebElementIdx(ref);
            if (prIdx < 0) {
                return OperationResponse.error("No web element index for ref: " + ref).toJson();
            }

            Activity activity = getValidActivity();
            if (activity == null) return noActivityError();

            String jsResult = WebViewExecutor.executeJs(activity, WebActionScript.selectOption(prIdx, value));
            if (jsResult == null) {
                return OperationResponse.error("JS execution returned null").toJson();
            }
            return jsResult;
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"ref\":\"w1\",\"value\":\"option1\"}").toJson();
        }
    }

    // ── Press Key ──────────────────────────────────────────

    public static String handlePressKey(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String key = json.getString("key");

            Activity activity = getValidActivity();
            if (activity == null) return noActivityError();

            switch (key) {
                case "back":
                    if (json.optBoolean("web", false)) {
                        // Web back via JS
                        String jsResult = WebViewExecutor.executeJs(activity, WebActionScript.goBack());
                        if (jsResult != null) return jsResult;
                    }
                    // Native back
                    activity.runOnUiThread(() -> activity.onBackPressed());
                    return OperationResponse.success("back").toJson();

                case "enter": {
                    int keyCode = android.view.KeyEvent.KEYCODE_ENTER;
                    activity.runOnUiThread(() -> {
                        long time = android.os.SystemClock.uptimeMillis();
                        activity.dispatchKeyEvent(new android.view.KeyEvent(
                                time, time, android.view.KeyEvent.ACTION_DOWN, keyCode, 0));
                        activity.dispatchKeyEvent(new android.view.KeyEvent(
                                time, time + 1, android.view.KeyEvent.ACTION_UP, keyCode, 0));
                    });
                    return OperationResponse.success("enter").toJson();
                }

                case "tab": {
                    int keyCode = android.view.KeyEvent.KEYCODE_TAB;
                    activity.runOnUiThread(() -> {
                        long time = android.os.SystemClock.uptimeMillis();
                        activity.dispatchKeyEvent(new android.view.KeyEvent(
                                time, time, android.view.KeyEvent.ACTION_DOWN, keyCode, 0));
                        activity.dispatchKeyEvent(new android.view.KeyEvent(
                                time, time + 1, android.view.KeyEvent.ACTION_UP, keyCode, 0));
                    });
                    return OperationResponse.success("tab").toJson();
                }

                default:
                    return OperationResponse.error("Unsupported key: " + key).toJson();
            }
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"key\":\"back\"}").toJson();
        }
    }

    // ── Helpers ────────────────────────────────────────────

    private static int[] getScreenCoordsOrError(String ref) {
        return RefBoundsCache.getScreenCoords(ref);
    }

    private static String coordsError(String ref) {
        return OperationResponse.error("Unknown ref: " + ref + ". Capture UI first.").toJson();
    }

    private static Activity getValidActivity() {
        Activity activity = ForegroundActivityTracker.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        return activity;
    }

    private static String noActivityError() {
        return OperationResponse.error("No foreground Activity").toJson();
    }

    private static float[] computeSwipeCoords(String direction, float cx, float cy, float margin) {
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

    private static int charToKeyCode(char c) {
        if (c >= 'a' && c <= 'z') return android.view.KeyEvent.KEYCODE_A + (c - 'a');
        if (c >= 'A' && c <= 'Z') return android.view.KeyEvent.KEYCODE_A + (c - 'A');
        if (c >= '0' && c <= '9') return android.view.KeyEvent.KEYCODE_0 + (c - '0');
        switch (c) {
            case ' ': return android.view.KeyEvent.KEYCODE_SPACE;
            case '.': return android.view.KeyEvent.KEYCODE_PERIOD;
            case ',': return android.view.KeyEvent.KEYCODE_COMMA;
            case '\n': return android.view.KeyEvent.KEYCODE_ENTER;
            case '\t': return android.view.KeyEvent.KEYCODE_TAB;
            case '@': return android.view.KeyEvent.KEYCODE_AT;
            case '_': return android.view.KeyEvent.KEYCODE_MINUS; // shift+minus
            default: return -1; // non-keyboard char, use ACTION_MULTIPLE
        }
    }
}
