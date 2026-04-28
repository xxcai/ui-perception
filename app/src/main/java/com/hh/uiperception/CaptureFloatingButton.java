package com.hh.uiperception;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.hh.uiperception.capture.CaptureExecutor;
import com.hh.uiperception.capture.CaptureRequest;
import com.hh.uiperception.capture.CaptureResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 抓取浮动按钮。
 * 使用 Application context + TYPE_APPLICATION_OVERLAY 创建独立窗口，
 * 不绑定任何 Activity 的 window token，因此不受 Activity 进出场动画影响。
 *
 * 参考 mobile-agent FloatingBallManager 的实现模式。
 */
public final class CaptureFloatingButton {

    /** 单例视图，整个应用只有一个浮动按钮 */
    private static View buttonView;
    private static WindowManager.LayoutParams layoutParams;
    private static WindowManager windowManager;
    private static boolean attached;
    private static boolean permissionToastShown;

    private CaptureFloatingButton() {
    }

    /**
     * 显示浮动抓取按钮。
     * 首次调用时创建视图并添加到 WindowManager，后续调用仅更新可见性。
     */
    public static void show(Context context) {
        if (!checkOverlayPermission(context)) {
            if (!permissionToastShown) {
                Toast.makeText(context.getApplicationContext(),
                        "需要授予「显示在其他应用上层」权限才能使用抓取按钮",
                        Toast.LENGTH_LONG).show();
                permissionToastShown = true;
            }
            return;
        }

        Context appContext = context.getApplicationContext();
        if (windowManager == null) {
            windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        }

        if (buttonView == null) {
            createButton(appContext);
        }

        if (!attached) {
            windowManager.addView(buttonView, layoutParams);
            attached = true;
        }
    }

    /**
     * 隐藏浮动抓取按钮。
     */
    public static void hide() {
        if (attached && buttonView != null) {
            windowManager.removeView(buttonView);
            attached = false;
        }
        permissionToastShown = false;
    }

    private static void createButton(Context appContext) {
        int size = dp(appContext, 48);

        buttonView = new View(appContext) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(0xFF1593FF);
                float radius = Math.min(getWidth(), getHeight()) / 2f;
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
            }
        };

        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.width = size;
        layoutParams.height = size;
        layoutParams.gravity = Gravity.BOTTOM | Gravity.END;
        layoutParams.x = dp(appContext, 16);
        layoutParams.y = dp(appContext, 96);
    }

    /**
     * 在给定 Activity 上触发抓取流程。
     * 通过 ActivityLifecycleCallbacks 调用，由 App 管理。
     */
    static void attachClickHandler(Activity activity) {
        if (buttonView != null) {
            buttonView.setOnClickListener(v -> executeCapture(activity));
        }
    }

    private static void executeCapture(Activity activity) {
        String baselineId = resolveBaselineId(activity);

        CaptureRequest request = new CaptureRequest(baselineId, "native_xml");
        List<CaptureResult> results = CaptureExecutor.execute(activity,
                Collections.singletonList(request));

        for (CaptureResult result : results) {
            if (result.isSuccess()) {
                writeToFile(activity, result);
            }
        }
    }

    private static void writeToFile(Context context, CaptureResult result) {
        File captureDir = new File(context.getExternalFilesDir(null), "captures/" + result.baselineId());
        captureDir.mkdirs();
        String filename = result.channelName() + "_" + result.timestampMs() + ".xml";
        File file = new File(captureDir, filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(result.content());
        } catch (IOException e) {
            // 静默失败，开发阶段暂不处理
        }
    }

    private static String resolveBaselineId(Activity activity) {
        String className = activity.getClass().getName();
        if (className.contains("NativeHome")) {
            return "native_home_message";
        }
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static boolean checkOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
}
