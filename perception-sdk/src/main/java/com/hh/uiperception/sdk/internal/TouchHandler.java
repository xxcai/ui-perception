package com.hh.uiperception.sdk.internal;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hh.uiperception.nativeplugin.WindowManagerHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class TouchHandler {

    private static final String TAG = "PerceptionSdk";
    private static final int CLICK_DURATION_MS = 50;
    private static final int SWIPE_DURATION_MS = 300;
    private static final int SWIPE_STEPS = 10;

    private TouchHandler() {}

    static boolean click(Activity activity, float x, float y) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        activity.runOnUiThread(() -> {
            try {
                long downTime = SystemClock.uptimeMillis();

                MotionEvent downEvent = MotionEvent.obtain(downTime, downTime,
                        MotionEvent.ACTION_DOWN, x, y, 0);
                result[0] = dispatchTouchEvent(activity, downEvent, x, y);
                downEvent.recycle();

                SystemClock.sleep(CLICK_DURATION_MS);

                MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + CLICK_DURATION_MS,
                        MotionEvent.ACTION_UP, x, y, 0);
                dispatchTouchEvent(activity, upEvent, x, y);
                upEvent.recycle();

                Log.i(TAG, "Click at (" + x + ", " + y + ")");
            } catch (Exception e) {
                Log.e(TAG, "Click failed", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            return latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static boolean longPress(Activity activity, float x, float y, int durationMs) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        activity.runOnUiThread(() -> {
            try {
                long downTime = SystemClock.uptimeMillis();

                MotionEvent downEvent = MotionEvent.obtain(downTime, downTime,
                        MotionEvent.ACTION_DOWN, x, y, 0);
                result[0] = dispatchTouchEvent(activity, downEvent, x, y);
                downEvent.recycle();

                SystemClock.sleep(durationMs);

                MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + durationMs,
                        MotionEvent.ACTION_UP, x, y, 0);
                dispatchTouchEvent(activity, upEvent, x, y);
                upEvent.recycle();

                Log.i(TAG, "Long press at (" + x + ", " + y + ") for " + durationMs + "ms");
            } catch (Exception e) {
                Log.e(TAG, "Long press failed", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            return latch.await(durationMs + 3000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    static boolean swipe(Activity activity, float startX, float startY, float endX, float endY) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        activity.runOnUiThread(() -> {
            try {
                long downTime = SystemClock.uptimeMillis();
                long stepTime = SWIPE_DURATION_MS / SWIPE_STEPS;

                MotionEvent downEvent = MotionEvent.obtain(downTime, downTime,
                        MotionEvent.ACTION_DOWN, startX, startY, 0);
                dispatchTouchEvent(activity, downEvent, startX, startY);
                downEvent.recycle();

                for (int i = 1; i <= SWIPE_STEPS; i++) {
                    float fraction = (float) i / SWIPE_STEPS;
                    float x = startX + (endX - startX) * fraction;
                    float y = startY + (endY - startY) * fraction;
                    long time = downTime + stepTime * i;

                    MotionEvent moveEvent = MotionEvent.obtain(downTime, time,
                            MotionEvent.ACTION_MOVE, x, y, 0);
                    dispatchTouchEvent(activity, moveEvent, x, y);
                    moveEvent.recycle();

                    SystemClock.sleep(stepTime);
                }

                long upTime = downTime + SWIPE_DURATION_MS;
                MotionEvent upEvent = MotionEvent.obtain(downTime, upTime,
                        MotionEvent.ACTION_UP, endX, endY, 0);
                dispatchTouchEvent(activity, upEvent, endX, endY);
                upEvent.recycle();

                result[0] = true;
                Log.i(TAG, "Swipe (" + startX + "," + startY + ") -> (" + endX + "," + endY + ")");
            } catch (Exception e) {
                Log.e(TAG, "Swipe failed", e);
            } finally {
                latch.countDown();
            }
        });

        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Dispatch a touch event to the focused window's root view.
     *
     * Caller passes SCREEN-ABSOLUTE coordinates (origin = top-left of physical screen).
     * The framework's dispatchTouchEvent treats the event X/Y as coords relative to the
     * receiving view, so we must subtract rootView's screen position to convert.
     *
     * Previously this method had two paths: activity.dispatchTouchEvent() (no conversion)
     * when rootView was the activity's own decorView, and direct rootView.dispatchTouchEvent()
     * with conversion otherwise. The no-conversion path was only correct when decorView
     * sat at screen (0, 0) — it produced a Y-offset bug for Dialog-themed activities,
     * multi-window, and any case where the window was placed below/right of (0, 0).
     */
    private static boolean dispatchTouchEvent(Activity activity, MotionEvent event, float screenX, float screenY) {
        View rootView = WindowManagerHelper.getFocusedWindowView(activity);
        if (rootView == null) {
            return activity.dispatchTouchEvent(event);
        }

        int[] loc = new int[2];
        rootView.getLocationOnScreen(loc);
        float localX = screenX - loc[0];
        float localY = screenY - loc[1];

        MotionEvent localEvent = MotionEvent.obtain(event);
        localEvent.setLocation(localX, localY);
        boolean result = rootView.dispatchTouchEvent(localEvent);
        localEvent.recycle();
        return result;
    }
}
