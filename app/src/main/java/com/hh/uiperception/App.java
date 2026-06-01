package com.hh.uiperception;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.hh.uiperception.sdk.PerceptionSdk;

public class App extends Application {

    private static final String TAG = "UIPerception";

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
                String className = activity.getClass().getName();
                if (className.contains("uiperception.baseline.")) {
                    CaptureFloatingButton.show(activity);
                    CaptureFloatingButton.attachClickHandler(activity);
                } else {
                    CaptureFloatingButton.hide();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });

        PerceptionSdk.startHttpServer(9700);
    }
}
