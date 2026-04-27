package com.hh.uiperception.baseline;

import android.content.Context;

public final class BaselineRouter {
    private BaselineRouter() {
    }

    public static boolean open(Context context, String route) {
        if (context == null || route == null || route.trim().isEmpty()) {
            return false;
        }

        // Step 2 only defines the routing boundary. Concrete routes are connected when
        // baseline pages are added in later steps.
        return false;
    }
}
