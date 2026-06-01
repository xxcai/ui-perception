package com.hh.uiperception.sdk;

import android.app.Application;
import android.util.Log;

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

    private PerceptionSdk() {}

    static void init(Application app) {
        ForegroundActivityTracker.register(app);
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

    public static OperationResponse click(String ref) {
        return OperationHandler.fromJson(OperationHandler.handleClick("{\"ref\":\"" + ref + "\"}"));
    }

    public static OperationResponse swipe(String direction, String ref) {
        String body = "{\"direction\":\"" + direction + "\"" + (ref != null ? ",\"ref\":\"" + ref + "\"" : "") + "}";
        return OperationHandler.fromJson(OperationHandler.handleSwipe(body));
    }
}
