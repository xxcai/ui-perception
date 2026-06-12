package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class OpenUriHandler {

    private static final String TAG = "PerceptionSdk";
    private static final String META_KEY = "OpenUriProvider";
    private static final int TIMEOUT_SECONDS = 5;

    private static Class<?> providerClass;
    private static boolean resolved;

    private OpenUriHandler() {}

    public static String handleOpenUri(String body) {
        String uri;
        try {
            JSONObject json = new JSONObject(body);
            uri = json.getString("uri");
        } catch (JSONException e) {
            return OperationResponse.error("Invalid request: expected {\"uri\":\"...\"}").toJson();
        }

        Activity activity = ForegroundActivityTracker.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return OperationResponse.error("No foreground Activity").toJson();
        }

        Application app = activity.getApplication();
        Class<?> cls = resolveProviderClass(app);
        if (cls == null) {
            return OperationResponse.error("OpenUriProvider not configured. Add <meta-data android:name=\"OpenUriProvider\" android:value=\"...\"/> to AndroidManifest.").toJson();
        }

        final String finalUri = uri;
        final Class<?> finalCls = cls;
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};
        final Exception[] error = {null};

        activity.runOnUiThread(() -> {
            try {
                Object instance = finalCls.newInstance();
                Method method = finalCls.getMethod("openUri", String.class);
                method.invoke(instance, finalUri);
                result[0] = true;
            } catch (Exception e) {
                Log.e(TAG, "openUri invocation failed", e);
                error[0] = e;
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return OperationResponse.error("openUri timed out after " + TIMEOUT_SECONDS + "s").toJson();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return OperationResponse.error("openUri interrupted").toJson();
        }

        if (error[0] != null) {
            return OperationResponse.error("openUri failed: " + error[0].getMessage()).toJson();
        }

        return "{\"status\":\"success\",\"result\":{\"uri\":\"" + OperationResponse.escape(uri) + "\"}}";
    }

    private static Class<?> resolveProviderClass(Application app) {
        if (resolved) return providerClass;
        resolved = true;

        try {
            ApplicationInfo info = app.getPackageManager()
                    .getApplicationInfo(app.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = info.metaData;
            if (metaData == null) return null;

            String className = metaData.getString(META_KEY);
            if (className == null || className.isEmpty()) return null;

            providerClass = Class.forName(className);
            Log.i(TAG, "OpenUriProvider resolved: " + className);
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve OpenUriProvider: " + e.getMessage());
        }
        return providerClass;
    }
}
