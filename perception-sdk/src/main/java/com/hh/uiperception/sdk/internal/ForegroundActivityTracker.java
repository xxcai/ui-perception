package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class ForegroundActivityTracker {

    private static volatile Activity foregroundActivity;
    private static Application.ActivityLifecycleCallbacks callbacks;

    private ForegroundActivityTracker() {}

    public static synchronized void register(Application app) {
        if (callbacks != null) return;
        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                foregroundActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (foregroundActivity == activity) {
                    foregroundActivity = null;
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (foregroundActivity == activity) {
                    foregroundActivity = null;
                }
            }
        };
        app.registerActivityLifecycleCallbacks(callbacks);
    }

    public static Activity getForegroundActivity() {
        return foregroundActivity;
    }
}
