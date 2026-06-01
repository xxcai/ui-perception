package com.hh.uiperception.florence2plugin.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hh.uiperception.florence2plugin.Florence2Detector;
import com.hh.uiperception.florence2plugin.Florence2Result;
import com.hh.uiperception.florence2plugin.IconCaptionResult;
import com.hh.uiperception.yoloplugin.YoloClassLabels;
import com.hh.uiperception.yoloplugin.YoloDetection;
import com.hh.uiperception.yoloplugin.YoloDetectionResult;
import com.hh.uiperception.yoloplugin.YoloDetector;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * YOLO + Florence-2 icon caption 管线调试页。
 * 流程：加载图片 → YOLO 检测 icon → 裁剪 → Florence-2 caption → 展示结果。
 */
public final class Florence2DebugActivity extends Activity {

    private static final String TAG = "Florence2Debug";
    private static final int REQUEST_CODE_PICK_IMAGE = 2001;

    private YoloDetector yoloDetector;
    private Florence2Detector florence2Detector;
    private boolean modelsLoaded = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView statusView;
    private Button loadModelsButton;
    private Button runPipelineButton;
    private Button pickButton;
    private ImageView imagePreview;
    private LinearLayout resultsContainer;
    private TextView summaryView;

    private Bitmap screenshotBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Florence-2 Icon Caption");
        setContentView(createContentView());
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        if (yoloDetector != null) { yoloDetector.close(); yoloDetector = null; }
        if (florence2Detector != null) { florence2Detector.close(); florence2Detector = null; }
        if (screenshotBitmap != null && !screenshotBitmap.isRecycled()) {
            screenshotBitmap.recycle();
            screenshotBitmap = null;
        }
        super.onDestroy();
    }

    // ---- Layout ----

    private ScrollView createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFFF5F5F5);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 状态栏
        statusView = new TextView(this);
        statusView.setText("请先加载模型，然后选择图片或使用内置测试图");
        statusView.setTextSize(14);
        statusView.setTextColor(0xFF333333);
        statusView.setPadding(0, 0, 0, dp(8));
        root.addView(statusView);

        // 按钮行 1：加载模型 + 选择图片
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setPadding(0, 0, 0, dp(4));

        loadModelsButton = new Button(this);
        loadModelsButton.setText("加载模型");
        loadModelsButton.setAllCaps(false);
        loadModelsButton.setOnClickListener(v -> loadModels());
        row1.addView(loadModelsButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        pickButton = new Button(this);
        pickButton.setText("选择图片");
        pickButton.setAllCaps(false);
        pickButton.setOnClickListener(v -> pickImage());
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pickParams.setMargins(dp(8), 0, 0, 0);
        row1.addView(pickButton, pickParams);

        root.addView(row1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 按钮行 2：运行管线
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, 0, 0, dp(12));

        runPipelineButton = new Button(this);
        runPipelineButton.setText("运行 Icon Caption 测试");
        runPipelineButton.setAllCaps(false);
        runPipelineButton.setEnabled(false);
        runPipelineButton.setOnClickListener(v -> runPipeline());
        row2.addView(runPipelineButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(row2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 图片预览
        imagePreview = new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setBackgroundColor(0xFFEEEEEE);
        imagePreview.setMinimumHeight(dp(250));
        root.addView(imagePreview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 汇总统计
        summaryView = new TextView(this);
        summaryView.setTextSize(13);
        summaryView.setTextColor(0xFF333333);
        summaryView.setBackgroundColor(0xFFFFFFFF);
        summaryView.setPadding(dp(12), dp(12), dp(12), dp(12));
        summaryView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        summaryParams.setMargins(0, dp(8), 0, 0);
        root.addView(summaryView, summaryParams);

        // 结果列表容器
        resultsContainer = new LinearLayout(this);
        resultsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultsContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return scrollView;
    }

    // ---- Actions ----

    private void loadModels() {
        loadModelsButton.setEnabled(false);
        statusView.setText("正在加载 YOLO + Florence-2 模型...");
        executor.execute(() -> {
            try {
                long start = System.currentTimeMillis();
                yoloDetector = new YoloDetector(this);
                long yoloMs = System.currentTimeMillis() - start;

                start = System.currentTimeMillis();
                florence2Detector = new Florence2Detector(this, false);
                long florence2Ms = System.currentTimeMillis() - start;

                modelsLoaded = true;
                runOnUiThread(() -> {
                    statusView.setText(String.format(Locale.US,
                            "模型已加载 (YOLO %dms + Florence-2 %dms)", yoloMs, florence2Ms));
                    runPipelineButton.setEnabled(true);

                    // 自动加载内置测试图
                    loadTestScreenshot();
                });
            } catch (Exception e) {
                Log.e(TAG, "Model load failed", e);
                runOnUiThread(() -> {
                    statusView.setText("加载失败: " + e.getMessage());
                    loadModelsButton.setEnabled(true);
                });
            }
        });
    }

    private void loadTestScreenshot() {
        try (InputStream is = getAssets().open("florence2-test/screenshot.jpg")) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                if (screenshotBitmap != null && !screenshotBitmap.isRecycled()) {
                    screenshotBitmap.recycle();
                }
                screenshotBitmap = bitmap;
                imagePreview.setImageBitmap(bitmap);
                statusView.setText(String.format(Locale.US, "%s\n内置测试图: %dx%d",
                        statusView.getText(), bitmap.getWidth(), bitmap.getHeight()));
            }
        } catch (Exception e) {
            Log.w(TAG, "No built-in test image: " + e.getMessage());
        }
    }

    private void runPipeline() {
        if (!modelsLoaded) return;
        runPipelineButton.setEnabled(false);
        resultsContainer.removeAllViews();
        summaryView.setText("正在运行...");
        statusView.setText("YOLO 检测中...");

        executor.execute(() -> {
            try {
                if (screenshotBitmap == null || screenshotBitmap.isRecycled()) {
                    runOnUiThread(() -> {
                        statusView.setText("无图片，请先选择图片");
                        runPipelineButton.setEnabled(true);
                    });
                    return;
                }

                // 1. YOLO 检测
                long yoloStart = System.currentTimeMillis();
                YoloDetectionResult yoloResult = yoloDetector.detect(screenshotBitmap);
                long yoloMs = System.currentTimeMillis() - yoloStart;

                List<YoloDetection> allDetections = yoloResult.detections;
                int totalDetected = allDetections.size();
                // 限制最多 caption 前 10 个，避免耗时过长
                List<YoloDetection> detections = allDetections.size() > 10
                        ? allDetections.subList(0, 10) : allDetections;
                Log.d(TAG, String.format("YOLO: %d detections (capped %d) in %dms",
                        totalDetected, detections.size(), yoloMs));

                if (detections.isEmpty()) {
                    runOnUiThread(() -> {
                        statusView.setText("YOLO 未检测到任何目标");
                        summaryView.setText(String.format("YOLO: %dms, 0 detections", yoloMs));
                        runPipelineButton.setEnabled(true);
                    });
                    return;
                }

                runOnUiThread(() -> statusView.setText(String.format(Locale.US,
                        "YOLO: %d targets in %dms, Florence-2 captioning...", detections.size(), yoloMs)));

                // 2. 画 YOLO 检测框到预览图
                Bitmap annotated = screenshotBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(annotated);
                Paint boxPaint = new Paint();
                boxPaint.setColor(Color.RED);
                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(3);
                for (YoloDetection det : detections) {
                    canvas.drawRect(det.x1, det.y1, det.x2, det.y2, boxPaint);
                }

                // 3. 裁剪 + Florence-2 caption
                List<IconCaptionResult> captionResults = new ArrayList<>();
                long captionStart = System.currentTimeMillis();

                for (int i = 0; i < detections.size(); i++) {
                    YoloDetection det = detections.get(i);

                    // 裁剪 bbox 区域
                    int x = Math.max(0, (int) det.x1);
                    int y = Math.max(0, (int) det.y1);
                    int w = Math.min(screenshotBitmap.getWidth() - x, (int) (det.x2 - det.x1));
                    int h = Math.min(screenshotBitmap.getHeight() - y, (int) (det.y2 - det.y1));
                    if (w <= 0 || h <= 0) continue;

                    Bitmap crop = Bitmap.createBitmap(screenshotBitmap, x, y, w, h);

                    // Florence-2 caption
                    long inferStart = System.currentTimeMillis();
                    String caption;
                    try {
                        Florence2Result r = florence2Detector.infer(crop);
                        caption = r.caption;
                    } catch (Exception e) {
                        Log.e(TAG, "Caption failed for #" + i, e);
                        caption = "(error: " + e.getMessage() + ")";
                    }
                    long inferMs = System.currentTimeMillis() - inferStart;

                    captionResults.add(new IconCaptionResult(
                            i, det.className, det.score,
                            det.x1, det.y1, det.x2, det.y2,
                            caption, inferMs, crop));
                }
                long totalCaptionMs = System.currentTimeMillis() - captionStart;
                long totalMs = yoloMs + totalCaptionMs;

                // 4. 显示结果
                Bitmap finalAnnotated = annotated;
                runOnUiThread(() -> {
                    imagePreview.setImageBitmap(finalAnnotated);
                    showResults(captionResults, yoloMs, totalCaptionMs, totalMs);
                    statusView.setText(String.format(Locale.US,
                            "完成: %d icons captioned, 总耗时 %dms",
                            captionResults.size(), totalMs));
                    runPipelineButton.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Pipeline failed", e);
                runOnUiThread(() -> {
                    statusView.setText("管线失败: " + e.getMessage());
                    runPipelineButton.setEnabled(true);
                });
            }
        });
    }

    private void showResults(List<IconCaptionResult> results, long yoloMs,
                             long captionMs, long totalMs) {
        // 汇总
        long avgMs = results.isEmpty() ? 0 : captionMs / results.size();
        summaryView.setText(String.format(Locale.US,
                "总耗时: %dms (YOLO %dms + Caption %dms)\n"
              + "Icons: %d 个, 平均单次 caption: %dms\n"
              + "模型输入: crop → resize 768×768 → Florence-2",
                totalMs, yoloMs, captionMs, results.size(), avgMs));

        // 每个 icon 的结果卡片
        for (IconCaptionResult r : results) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundColor(0xFFFFFFFF);
            card.setPadding(dp(8), dp(8), dp(8), dp(8));
            card.setGravity(Gravity.CENTER_VERTICAL);

            // 裁剪缩略图
            ImageView cropView = new ImageView(this);
            cropView.setImageBitmap(r.cropBitmap);
            cropView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            cropView.setBackgroundColor(0xFFEEEEEE);
            int thumbSize = dp(64);
            card.addView(cropView, new LinearLayout.LayoutParams(thumbSize, thumbSize));

            // 文字信息
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(dp(8), 0, 0, 0);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            card.addView(textCol, textParams);

            TextView captionText = new TextView(this);
            captionText.setText(String.format(Locale.US, "#%d %s (%.2f)\nCaption: %s\n%dms",
                    r.index, r.yoloClass, r.yoloConfidence, r.caption, r.florence2Ms));
            captionText.setTextSize(12);
            captionText.setTextColor(0xFF333333);
            textCol.addView(captionText);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, dp(2), 0, dp(2));
            resultsContainer.addView(card, cardParams);
        }
    }

    // ---- Image picking ----

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择截图"), REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            executor.execute(() -> {
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
                    if (bitmap == null) {
                        runOnUiThread(() -> statusView.setText("无法解码图片"));
                        return;
                    }
                    runOnUiThread(() -> {
                        if (screenshotBitmap != null && !screenshotBitmap.isRecycled()) {
                            screenshotBitmap.recycle();
                        }
                        screenshotBitmap = bitmap;
                        imagePreview.setImageBitmap(bitmap);
                        statusView.setText(String.format(Locale.US,
                                "已加载图片: %dx%d", bitmap.getWidth(), bitmap.getHeight()));
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> statusView.setText("加载图片失败: " + e.getMessage()));
                }
            });
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
