package com.hh.uiperception;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.hh.uiperception.nativeplugin.NativeXmlPlugin;

/**
 * 应用入口。
 * 注册抓取插件，并通过 ActivityLifecycleCallbacks 管理浮动抓取按钮的可见性。
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册抓取通道
        new NativeXmlPlugin().register();

        // 管理浮动抓取按钮的可见性和点击处理
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
    }
}
