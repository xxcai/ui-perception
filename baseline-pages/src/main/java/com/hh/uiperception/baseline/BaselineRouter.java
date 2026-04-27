package com.hh.uiperception.baseline;

import android.content.Context;
import android.content.Intent;

import com.hh.uiperception.baseline.nativepage.NativeHomeActivity;
import com.hh.uiperception.baseline.web.WebBaselinePlaceholderActivity;

public final class BaselineRouter {
    private BaselineRouter() {
    }

    public static boolean open(Context context, String route) {
        if (context == null || route == null || route.trim().isEmpty()) {
            return false;
        }

        if (BaselineRoutes.NATIVE_HOME_MESSAGE.equals(route)) {
            Intent intent = new Intent(context, NativeHomeActivity.class);
            intent.putExtra(NativeHomeActivity.EXTRA_ROUTE, route);
            context.startActivity(intent);
            return true;
        } else if (BaselineRoutes.WEB_HOME_PLACEHOLDER.equals(route)) {
            context.startActivity(new Intent(context, WebBaselinePlaceholderActivity.class));
            return true;
        }

        return false;
    }
}
