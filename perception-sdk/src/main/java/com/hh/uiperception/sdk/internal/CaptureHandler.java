package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.util.Log;

import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionComposer;
import com.hh.uiperception.core.PerceptionEntryResult;
import com.hh.uiperception.core.PerceptionPlan;
import com.hh.uiperception.core.PerceptionRunResult;
import com.hh.uiperception.core.TransformResult;
import com.hh.uiperception.nativeplugin.NativePerceptionPlugin;

import java.util.Collections;

public final class CaptureHandler {

    private static final String TAG = "PerceptionSdk";

    public static CaptureResponse capture() {
        Activity activity = ForegroundActivityTracker.getForegroundActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return CaptureResponse.error("No foreground Activity");
        }

        try {
            PerceptionPlan plan = new PerceptionPlan(
                    activity.getClass().getSimpleName(),
                    Collections.singletonList(new NativePerceptionPlugin()),
                    true
            );
            PerceptionRunResult runResult = PerceptionComposer.execute(activity, plan);

            for (PerceptionEntryResult entry : runResult.entries()) {
                CaptureResult capture = entry.captureResult();
                if (capture == null || !capture.isSuccess()) continue;

                TransformResult transform = entry.transformResult();
                if (transform != null && transform.isSuccess()) {
                    CaptureResponse response = CaptureResponse.success(capture.activityClassName(), transform.content());
                    RefBoundsCache.update(transform.content());
                    return response;
                }
            }
            return CaptureResponse.error("Capture pipeline produced no result");
        } catch (Exception e) {
            Log.e(TAG, "Capture failed", e);
            return CaptureResponse.error("Capture failed: " + e.getMessage());
        }
    }
}
