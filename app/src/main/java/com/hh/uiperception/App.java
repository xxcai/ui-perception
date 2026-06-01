package com.hh.uiperception;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.hh.uiperception.portal.PerceptionHttpServer;

public class App extends Application {

    private static final String TAG = "UIPerception";
    private static volatile Activity foregroundActivity;
    private final PerceptionHttpServer httpServer = new PerceptionHttpServer();

    public static Activity getForegroundActivity() {
        return foregroundActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                foregroundActivity = activity;

                String className = activity.getClass().getName();
                if (className.contains("uiperception.baseline.")) {
                    CaptureFloatingButton.show(activity);
                    CaptureFloatingButton.attachClickHandler(activity);
                } else {
                    CaptureFloatingButton.hide();
                }
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
        });

        httpServer.start(9700);
        Log.i(TAG, "PerceptionHttpServer started on port 9700");
    }
}
