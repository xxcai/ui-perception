package com.hh.uiperception.sdk;

import android.app.Application;
import android.util.Log;

import com.hh.uiperception.core.PluginRegistry;
import com.hh.uiperception.core.WebFusionMode;
import com.hh.uiperception.nativeplugin.NativePerceptionPlugin;
import com.hh.uiperception.webplugin.WebPerceptionPlugin;
import com.hh.uiperception.sdk.internal.CaptureHandler;
import com.hh.uiperception.sdk.internal.CaptureResponse;
import com.hh.uiperception.sdk.internal.ForegroundActivityTracker;
import com.hh.uiperception.sdk.internal.OperationHandler;
import com.hh.uiperception.sdk.internal.OperationResponse;
import com.hh.uiperception.sdk.internal.PerceptionHttpServer;

public final class PerceptionSdk {

    private static final String TAG = "PerceptionSdk";
    private static final int DEFAULT_PORT = 9700;
    private static PerceptionHttpServer httpServer;
    private static WebFusionMode fusionMode = WebFusionMode.FUSION;

    private PerceptionSdk() {}

    static void init(Application app) {
        ForegroundActivityTracker.register(app);
        PluginRegistry.register(new NativePerceptionPlugin());
        PluginRegistry.register(new WebPerceptionPlugin());
        Log.i(TAG, "PerceptionSdk initialized");
    }

    public static CaptureResponse capture() {
        return CaptureHandler.capture();
    }

    public static void startHttpServer() {
        startHttpServer(DEFAULT_PORT);
    }

    public static synchronized void startHttpServer(int port) {
        if (httpServer != null && httpServer.isRunning()) {
            Log.w(TAG, "HTTP server already running");
            return;
        }
        if (httpServer == null) {
            httpServer = new PerceptionHttpServer();
        }
        httpServer.start(port);
    }

    public static synchronized void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    public static boolean isHttpServerRunning() {
        return httpServer != null && httpServer.isRunning();
    }

    // ── Operations ─────────────────────────────────────────

    public static OperationResponse click(String ref) {
        return OperationHandler.fromJson(OperationHandler.handleClick("{\"ref\":\"" + ref + "\"}"));
    }

    public static OperationResponse longPress(String ref, int durationMs) {
        return OperationHandler.fromJson(OperationHandler.handleLongPress(
                "{\"ref\":\"" + ref + "\",\"duration\":" + durationMs + "}"));
    }

    public static OperationResponse swipe(String direction, String ref) {
        String body = "{\"direction\":\"" + direction + "\"" + (ref != null ? ",\"ref\":\"" + ref + "\"" : "") + "}";
        return OperationHandler.fromJson(OperationHandler.handleSwipe(body));
    }

    public static OperationResponse typeText(String ref, String text, boolean clear) {
        String body = "{\"ref\":\"" + ref + "\",\"text\":" + escapeJsonString(text) + ",\"clear\":" + clear + "}";
        return OperationHandler.fromJson(OperationHandler.handleTypeText(body));
    }

    public static OperationResponse check(String ref) {
        return OperationHandler.fromJson(OperationHandler.handleCheck("{\"ref\":\"" + ref + "\"}"));
    }

    public static OperationResponse uncheck(String ref) {
        return OperationHandler.fromJson(OperationHandler.handleUncheck("{\"ref\":\"" + ref + "\"}"));
    }

    public static OperationResponse selectOption(String ref, String value) {
        String body = "{\"ref\":\"" + ref + "\",\"value\":" + escapeJsonString(value) + "}";
        return OperationHandler.fromJson(OperationHandler.handleSelectOption(body));
    }

    public static OperationResponse pressKey(String key) {
        return OperationHandler.fromJson(OperationHandler.handlePressKey("{\"key\":\"" + key + "\"}"));
    }

    // ── Config ─────────────────────────────────────────────

    public static void setFusionMode(WebFusionMode mode) {
        if (mode != null) {
            fusionMode = mode;
        }
    }

    public static WebFusionMode fusionMode() {
        return fusionMode;
    }

    private static String escapeJsonString(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
