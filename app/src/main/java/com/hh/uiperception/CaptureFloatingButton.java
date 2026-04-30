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

import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionEntryResult;
import com.hh.uiperception.core.PerceptionComposer;
import com.hh.uiperception.core.PerceptionPlan;
import com.hh.uiperception.core.PerceptionRunResult;
import com.hh.uiperception.core.TransformResult;
import com.hh.uiperception.baseline.BaselineRoutes;
import com.hh.uiperception.baseline.nativepage.NativeHomeActivity;
import com.hh.uiperception.nativeplugin.NativePerceptionPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

/**
 * 感知浮动按钮。
 * 使用 Application context + TYPE_APPLICATION_OVERLAY 创建独立窗口，
 * 不绑定任何 Activity 的 window token，因此不受 Activity 进出场动画影响。
 *
 * 参考 mobile-agent FloatingBallManager 的实现模式。
 */
public final class CaptureFloatingButton {

    private static final NativePerceptionPlugin NATIVE_PLUGIN = new NativePerceptionPlugin();

    /** 单例视图，整个应用只有一个浮动按钮 */
    private static View buttonView;
    private static WindowManager.LayoutParams layoutParams;
    private static WindowManager windowManager;
    private static boolean attached;
    private static boolean permissionToastShown;

    private CaptureFloatingButton() {
    }

    /**
     * 显示浮动感知按钮。
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
     * 隐藏浮动感知按钮。
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
     * 在给定 Activity 上触发感知流程。
     * 通过 ActivityLifecycleCallbacks 调用，由 App 管理。
     */
    static void attachClickHandler(Activity activity) {
        if (buttonView != null) {
            buttonView.setOnClickListener(v -> executePerception(activity));
        }
    }

    private static void executePerception(Activity activity) {
        String baselineId = resolveBaselineId(activity);

        PerceptionPlan plan = new PerceptionPlan(
                baselineId,
                Collections.singletonList(NATIVE_PLUGIN),
                true
        );
        PerceptionRunResult runResult = PerceptionComposer.execute(activity, plan);

        for (PerceptionEntryResult entry : runResult.entries()) {
            CaptureResult captureResult = entry.captureResult();
            if (captureResult != null && captureResult.isSuccess()) {
                writeCaptureToFile(activity, runResult, entry.pluginName(), captureResult);
            }
            TransformResult transformResult = entry.transformResult();
            if (transformResult != null && transformResult.isSuccess()) {
                writeTransformToFile(activity, runResult, entry.pluginName(), transformResult);
            }
        }
    }

    private static void writeCaptureToFile(Context context, PerceptionRunResult runResult,
                                           String pluginName, CaptureResult result) {
        File captureDir = new File(context.getExternalFilesDir(null),
                "captures/" + runResult.baselineId() + "/runs/" + runResult.runId()
                        + "/" + pluginName + "/raw");
        captureDir.mkdirs();
        String filename = result.channelName() + "_" + result.timestampMs()
                + extensionFor(result.contentType());
        File file = new File(captureDir, filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(result.content());
        } catch (IOException e) {
            // 静默失败，开发阶段暂不处理
        }
    }

    private static void writeTransformToFile(Context context, PerceptionRunResult runResult,
                                             String pluginName, TransformResult result) {
        File transformDir = new File(context.getExternalFilesDir(null),
                "captures/" + runResult.baselineId() + "/runs/" + runResult.runId()
                        + "/" + pluginName + "/transformed");
        transformDir.mkdirs();
        String filename = result.toolName() + "_" + result.timestampMs()
                + extensionFor(result.contentType());
        File file = new File(transformDir, filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(result.content());
        } catch (IOException e) {
            // 静默失败，开发阶段暂不处理
        }
    }

    private static String resolveBaselineId(Activity activity) {
        if (activity instanceof NativeHomeActivity) {
            String route = activity.getIntent().getStringExtra(NativeHomeActivity.EXTRA_ROUTE);
            if (BaselineRoutes.NATIVE_HOME_MAIL.equals(route)) {
                return "native_home_mail";
            } else if (BaselineRoutes.NATIVE_HOME_CONTACTS.equals(route)) {
                return "native_home_contacts";
            } else if (BaselineRoutes.NATIVE_HOME_WORK.equals(route)) {
                return "native_home_business";
            }
            return "native_home_message";
        }

        String className = activity.getClass().getName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static String extensionFor(String contentType) {
        if ("text/xml".equals(contentType)) {
            return ".xml";
        } else if ("application/json".equals(contentType)) {
            return ".json";
        } else if ("text/html".equals(contentType)) {
            return ".html";
        }
        return ".txt";
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
