package com.hh.uiperception.nativeplugin;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class WindowManagerHelper {

    private WindowManagerHelper() {}

    static View getFocusedWindowView(Activity activity) {
        try {
            Object wmGlobal = getGlobalInstance();
            if (wmGlobal == null) return fallback(activity);

            List<View> views = readViews(wmGlobal);
            if (views == null || views.isEmpty()) return fallback(activity);

            // mViews is per-process, all views belong to this app.
            // Search from end (most recently added = topmost). Return the first
            // view with window focus — this covers Dialog (type=APPLICATION),
            // PopupWindow (type=APPLICATION_PANEL), and the main Activity window.
            for (int i = views.size() - 1; i >= 0; i--) {
                View v = views.get(i);
                if (v != null && v.hasWindowFocus()) return v;
            }
        } catch (Exception ignored) {
        }
        return fallback(activity);
    }

    private static View fallback(Activity activity) {
        if (activity != null && activity.getWindow() != null) {
            return activity.getWindow().getDecorView();
        }
        return null;
    }

    private static Object getGlobalInstance() {
        try {
            Class<?> clazz = Class.forName("android.view.WindowManagerGlobal");
            Method getInstance = clazz.getDeclaredMethod("getInstance");
            return getInstance.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<View> readViews(Object wmGlobal) {
        try {
            Field field = wmGlobal.getClass().getDeclaredField("mViews");
            field.setAccessible(true);
            Object value = field.get(wmGlobal);
            if (value instanceof List) return (List<View>) value;
            if (value instanceof View[]) {
                View[] arr = (View[]) value;
                List<View> list = new ArrayList<>(arr.length);
                for (View v : arr) list.add(v);
                return list;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
