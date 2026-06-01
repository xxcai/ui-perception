package com.hh.uiperception.yoloplugin.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hh.uiperception.yoloplugin.YoloAnnotator;
import com.hh.uiperception.yoloplugin.YoloClassLabels;
import com.hh.uiperception.yoloplugin.YoloDetection;
import com.hh.uiperception.yoloplugin.YoloDetectionResult;
import com.hh.uiperception.yoloplugin.YoloDetector;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * YOLO 检测调试页。
 * 独立于主感知流程，用于验证 YOLO 模型的识别能力。
 */
public final class YoloDebugActivity extends Activity {

    private static final String TAG = "YoloDebug";
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;

    private YoloDetector detector;
    private boolean modelLoaded = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView statusView;
    private SeekBar confSeekBar;
    private TextView confLabel;
    private SeekBar iouSeekBar;
    private TextView iouLabel;
    private Button loadButton;
    private Button detectButton;
    private Button screenshotButton;
    private Button pickButton;
    private ImageView imagePreview;
    private TextView statsView;
    private TextView detailView;

    private Bitmap currentBitmap;
    private Bitmap annotatedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("YOLO 检测调试");
        setContentView(createContentView());
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        if (detector != null) {
            detector.close();
            detector = null;
        }
        recycleBitmaps();
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
        statusView.setText("模型未加载");
        statusView.setTextSize(16);
        statusView.setTextColor(0xFF333333);
        statusView.setPadding(0, 0, 0, dp(8));
        root.addView(statusView);

        // 操作按钮行 1：加载模型 + 选择图片
        LinearLayout buttonRow1 = new LinearLayout(this);
        buttonRow1.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow1.setPadding(0, 0, 0, dp(4));

        loadButton = new Button(this);
        loadButton.setText("加载模型");
        loadButton.setAllCaps(false);
        loadButton.setOnClickListener(v -> loadModel());
        buttonRow1.addView(loadButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        pickButton = new Button(this);
        pickButton.setText("选择图片");
        pickButton.setAllCaps(false);
        pickButton.setOnClickListener(v -> pickImage());
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pickParams.setMargins(dp(8), 0, 0, 0);
        buttonRow1.addView(pickButton, pickParams);

        root.addView(buttonRow1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 操作按钮行 2：检测 + 截屏检测
        LinearLayout buttonRow2 = new LinearLayout(this);
        buttonRow2.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow2.setPadding(0, 0, 0, dp(12));

        detectButton = new Button(this);
        detectButton.setText("检测");
        detectButton.setAllCaps(false);
        detectButton.setEnabled(false);
        detectButton.setOnClickListener(v -> runDetection());
        buttonRow2.addView(detectButton, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        screenshotButton = new Button(this);
        screenshotButton.setText("截屏检测");
        screenshotButton.setAllCaps(false);
        screenshotButton.setEnabled(false);
        screenshotButton.setOnClickListener(v -> detectFromScreenshot());
        LinearLayout.LayoutParams screenshotParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        screenshotParams.setMargins(dp(8), 0, 0, 0);
        buttonRow2.addView(screenshotButton, screenshotParams);

        root.addView(buttonRow2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 置信度阈值
        LinearLayout confRow = new LinearLayout(this);
        confRow.setOrientation(LinearLayout.HORIZONTAL);
        confRow.setGravity(Gravity.CENTER_VERTICAL);
        confRow.setPadding(0, 0, 0, dp(8));

        TextView confTitle = new TextView(this);
        confTitle.setText("置信度: ");
        confTitle.setTextSize(14);
        confRow.addView(confTitle);

        confLabel = new TextView(this);
        confLabel.setText("0.15");
        confLabel.setTextSize(14);
        confLabel.setTextColor(0xFF1E88E5);
        confLabel.setMinWidth(dp(48));
        confRow.addView(confLabel);

        confSeekBar = new SeekBar(this);
        confSeekBar.setMax(90);
        confSeekBar.setProgress(10);
        confSeekBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = (progress + 5) / 100f;
                confLabel.setText(String.format(Locale.US, "%.2f", value));
                if (detector != null) detector.setConfThreshold(value);
            }
        });
        LinearLayout.LayoutParams confSeekParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        confRow.addView(confSeekBar, confSeekParams);
        root.addView(confRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // IoU 阈值
        LinearLayout iouRow = new LinearLayout(this);
        iouRow.setOrientation(LinearLayout.HORIZONTAL);
        iouRow.setGravity(Gravity.CENTER_VERTICAL);
        iouRow.setPadding(0, 0, 0, dp(12));

        TextView iouTitle = new TextView(this);
        iouTitle.setText("IoU:     ");
        iouTitle.setTextSize(14);
        iouRow.addView(iouTitle);

        iouLabel = new TextView(this);
        iouLabel.setText("0.45");
        iouLabel.setTextSize(14);
        iouLabel.setTextColor(0xFF1E88E5);
        iouLabel.setMinWidth(dp(48));
        iouRow.addView(iouLabel);

        iouSeekBar = new SeekBar(this);
        iouSeekBar.setMax(90);
        iouSeekBar.setProgress(40);
        iouSeekBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = (progress + 5) / 100f;
                iouLabel.setText(String.format(Locale.US, "%.2f", value));
                if (detector != null) detector.setIouThreshold(value);
            }
        });
        LinearLayout.LayoutParams iouSeekParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        iouRow.addView(iouSeekBar, iouSeekParams);
        root.addView(iouRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 图片预览
        imagePreview = new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setBackgroundColor(0xFFEEEEEE);
        imagePreview.setMinimumHeight(dp(300));
        root.addView(imagePreview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // 统计面板
        statsView = new TextView(this);
        statsView.setTextSize(13);
        statsView.setTextColor(0xFF333333);
        statsView.setBackgroundColor(0xFFFFFFFF);
        statsView.setPadding(dp(12), dp(12), dp(12), dp(12));
        statsView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        statsParams.setMargins(0, dp(8), 0, 0);
        root.addView(statsView, statsParams);

        // 详细检测列表
        detailView = new TextView(this);
        detailView.setTextSize(11);
        detailView.setTextColor(0xFF555555);
        detailView.setBackgroundColor(0xFFFFFFFF);
        detailView.setPadding(dp(12), dp(12), dp(12), dp(12));
        detailView.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(4), 0, 0);
        root.addView(detailView, detailParams);

        return scrollView;
    }

    // ---- Actions ----

    private void loadModel() {
        loadButton.setEnabled(false);
        statusView.setText("正在加载模型...");
        executor.execute(() -> {
            try {
                detector = new YoloDetector(YoloDebugActivity.this);
                detector.setConfThreshold(seekToConf(confSeekBar.getProgress()));
                detector.setIouThreshold(seekToIou(iouSeekBar.getProgress()));
                modelLoaded = true;
                runOnUiThread(() -> {
                    statusView.setText("模型已加载（" + YoloClassLabels.NUM_CLASSES + " 类）");
                    loadButton.setEnabled(false);
                    detectButton.setEnabled(true);
                    screenshotButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusView.setText("加载失败: " + e.getMessage());
                    loadButton.setEnabled(true);
                });
            }
        });
    }

    private void runDetection() {
        if (!modelLoaded) return;
        detectButton.setEnabled(false);
        statusView.setText("正在检测...");

        executor.execute(() -> {
            try {
                // 使用内置测试图片
                if (currentBitmap == null || currentBitmap.isRecycled()) {
                    currentBitmap = loadTestBitmap();
                }
                if (currentBitmap == null) {
                    runOnUiThread(() -> {
                        statusView.setText("无测试图片，请先截屏");
                        detectButton.setEnabled(true);
                    });
                    return;
                }

                YoloDetectionResult result = detector.detect(currentBitmap);
                Bitmap annotated = YoloAnnotator.annotate(currentBitmap, result.detections);

                runOnUiThread(() -> {
                    recycleAnnotated();
                    annotatedBitmap = annotated;
                    imagePreview.setImageBitmap(annotated);
                    updateStats(result);
                    updateDetail(result);
                    statusView.setText(String.format(Locale.US,
                            "检测完成: %d 个目标, 耗时 %dms",
                            result.detections.size(), result.totalMs()));
                    detectButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusView.setText("检测失败: " + e.getMessage());
                    detectButton.setEnabled(true);
                });
            }
        });
    }

    private void detectFromScreenshot() {
        if (!modelLoaded) return;
        screenshotButton.setEnabled(false);
        statusView.setText("正在截屏...");

        // 使用 PixelCopy 截取当前 Activity 的根视图
        View rootView = getWindow().getDecorView().getRootView();
        Bitmap screenshot = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(),
                Bitmap.Config.ARGB_8888);

        PixelCopy.request(getWindow(), screenshot, (int result) -> {
            if (result == PixelCopy.SUCCESS) {
                if (currentBitmap != null && currentBitmap != screenshot) {
                    currentBitmap.recycle();
                }
                currentBitmap = screenshot;
                runOnUiThread(() -> {
                    statusView.setText("截屏完成，开始检测...");
                    screenshotButton.setEnabled(true);
                    runDetection();
                });
            } else {
                screenshot.recycle();
                runOnUiThread(() -> {
                    statusView.setText("截屏失败: " + result);
                    screenshotButton.setEnabled(true);
                });
            }
        }, getMainHandler());
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
                        if (currentBitmap != null && !currentBitmap.isRecycled()) {
                            currentBitmap.recycle();
                        }
                        currentBitmap = bitmap;
                        statusView.setText(String.format(Locale.US,
                                "已加载图片: %dx%d", bitmap.getWidth(), bitmap.getHeight()));
                        if (modelLoaded) {
                            runDetection();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> statusView.setText("加载图片失败: " + e.getMessage()));
                }
            });
        }
    }

    // ---- Helpers ----

    private Bitmap loadTestBitmap() {
        // 尝试从 assets 加载测试图片
        String[] testPaths = {"yolo-test/test.png", "yolo-test/test.jpg"};
        for (String path : testPaths) {
            try (InputStream is = getAssets().open(path)) {
                return BitmapFactory.decodeStream(is);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void updateStats(YoloDetectionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "总检测: %d 个目标  |  耗时: %dms (预处理 %d + 推理 %d + 后处理 %d)\n\n",
                result.detections.size(), result.totalMs(),
                result.preprocessMs, result.inferenceMs, result.postprocessMs));

        // 按类别统计
        Map<String, Integer> classCounts = new HashMap<>();
        for (YoloDetection det : result.detections) {
            String name = YoloClassLabels.englishName(det.classId);
            classCounts.merge(name, 1, Integer::sum);
        }
        sb.append("按类别统计:\n");
        classCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> sb.append(String.format("  %-22s %d\n", e.getKey(), e.getValue())));

        statsView.setText(sb.toString());
    }

    private void updateDetail(YoloDetectionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%-4s %-20s %-6s %-8s %-30s\n",
                "#", "类别", "置信度", "大小", "坐标"));
        sb.append("─".repeat(80)).append("\n");

        int idx = 1;
        for (YoloDetection det : result.detections) {
            sb.append(String.format(Locale.US, "%-4d %-20s %.3f  %dx%-5d  [%.0f,%.0f,%.0f,%.0f]\n",
                    idx++,
                    YoloClassLabels.englishName(det.classId),
                    det.score,
                    (int) det.width(), (int) det.height(),
                    det.x1, det.y1, det.x2, det.y2));
        }
        detailView.setText(sb.toString());
    }

    private void recycleBitmaps() {
        if (currentBitmap != null) { currentBitmap.recycle(); currentBitmap = null; }
        recycleAnnotated();
    }

    private void recycleAnnotated() {
        if (annotatedBitmap != null) { annotatedBitmap.recycle(); annotatedBitmap = null; }
    }

    private float seekToConf(int progress) { return (progress + 5) / 100f; }
    private float seekToIou(int progress) { return (progress + 5) / 100f; }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private android.os.Handler getMainHandler() {
        return new android.os.Handler(getMainLooper());
    }

    private static abstract class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
