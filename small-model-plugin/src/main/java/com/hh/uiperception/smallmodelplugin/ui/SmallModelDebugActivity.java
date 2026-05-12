package com.hh.uiperception.smallmodelplugin.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.hh.uiperception.smallmodelplugin.api.SmallModelCallback;
import com.hh.uiperception.smallmodelplugin.api.SmallModelError;
import com.hh.uiperception.smallmodelplugin.api.SmallModelInitConfig;
import com.hh.uiperception.smallmodelplugin.experiment.IconBounds;
import com.hh.uiperception.smallmodelplugin.experiment.IconExperimentJson;
import com.hh.uiperception.smallmodelplugin.experiment.IconExperimentPromptBuilder;
import com.hh.uiperception.smallmodelplugin.experiment.IconExperimentResultStore;
import com.hh.uiperception.smallmodelplugin.experiment.IconExperimentRunResult;
import com.hh.uiperception.smallmodelplugin.experiment.IconExperimentRunner;
import com.hh.uiperception.smallmodelplugin.experiment.IconExperimentTestSet;
import com.hh.uiperception.smallmodelplugin.experiment.IconInputMode;
import com.hh.uiperception.smallmodelplugin.experiment.IconMatchResult;
import com.hh.uiperception.smallmodelplugin.experiment.IconMontageLayoutCalculator;
import com.hh.uiperception.smallmodelplugin.experiment.IconResultMatcher;
import com.hh.uiperception.smallmodelplugin.experiment.IconTarget;
import com.hh.uiperception.smallmodelplugin.experiment.IconTargetMapping;
import android.util.Log;

import com.hh.uiperception.smallmodelplugin.gemma.Gemma4E2BClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gemma 小模型测评页（上下+左右混合布局）。
 */
public final class SmallModelDebugActivity extends Activity {

    private static final String TAG = "SmallModelDebug";
    private static final String BUILTIN_FIXTURE_DIR =
            "icon-experiment/welink_message_001";
    private static final int EXPERIMENT_MAX_TOKENS = 2048;
    private static final int EXPERIMENT_TOP_K = 1;
    private static final double EXPERIMENT_TOP_P = 0.1d;
    private static final double EXPERIMENT_TEMPERATURE = 0.0d;

    private final Gemma4E2BClient client = new Gemma4E2BClient();
    private final ExecutorService experimentExecutor = Executors.newSingleThreadExecutor();

    private TextView statusView;
    private Switch gpuSwitch;
    private Spinner inputModeSpinner;
    private Spinner maxEdgeSpinner;
    private Button initButton;
    private Button runButton;
    private ImageView imagePreview;
    private TextView resultTable;
    private TextView timingView;
    private TextView promptPreview;
    private TextView rawOutputView;
    private TextView historyView;

    private Bitmap fixtureBitmap;
    private IconExperimentTestSet fixtureTestSet;
    private long lastLoadLatencyMs = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Gemma 测评");
        setContentView(createContentView());
        loadBuiltinFixture();
    }

    @Override
    protected void onDestroy() {
        experimentExecutor.shutdownNow();
        client.close();
        super.onDestroy();
    }

    // ---- Layout ----

    private ScrollView createContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Top bar
        root.addView(createTopBar(), matchWrap());

        // Control bar
        root.addView(createControlBar(), matchWrap());

        // Content area: left (45%) + right (55%)
        LinearLayout contentRow = new LinearLayout(this);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setWeightSum(1f);
        contentRow.addView(createLeftPanel(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.45f));
        contentRow.addView(createRightPanel(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.55f));
        root.addView(contentRow, matchWrap());

        return scrollView;
    }

    private LinearLayout createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setText("状态：未初始化");
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(statusView, statusParams);

        gpuSwitch = new Switch(this);
        gpuSwitch.setText("GPU");
        gpuSwitch.setChecked(true);
        bar.addView(gpuSwitch, wrapWrap());

        initButton = new Button(this);
        initButton.setText("加载模型");
        initButton.setOnClickListener(v -> initializeModel());
        bar.addView(initButton, wrapWrap());

        return bar;
    }

    private LinearLayout createControlBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, dp(8), 0, dp(8));

        TextView modeLabel = new TextView(this);
        modeLabel.setText("模式");
        modeLabel.setTextSize(13);
        bar.addView(modeLabel, wrapWrap());

        inputModeSpinner = spinner(new String[]{
                IconInputMode.FULL_IMAGE.name(),
                IconInputMode.FULL_IMAGE_WITH_BOUNDS.name(),
                IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS.name(),
                IconInputMode.CROPPED_MONTAGE.name()
        });
        inputModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                updateImagePreview();
                updatePromptPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        bar.addView(inputModeSpinner, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView edgeLabel = new TextView(this);
        edgeLabel.setText("长边");
        edgeLabel.setTextSize(13);
        bar.addView(edgeLabel, wrapWrap());

        maxEdgeSpinner = spinner(new String[]{"原图", "1024", "768", "512"});
        bar.addView(maxEdgeSpinner, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        runButton = new Button(this);
        runButton.setText("运行");
        runButton.setOnClickListener(v -> runExperiment());
        bar.addView(runButton, wrapWrap());

        return bar;
    }

    private LinearLayout createLeftPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 0, dp(6), 0);

        imagePreview = new ImageView(this);
        imagePreview.setAdjustViewBounds(true);
        imagePreview.setMinimumHeight(dp(180));
        imagePreview.setMaxHeight(dp(280));
        panel.addView(imagePreview, matchWrap());

        resultTable = outputText();
        resultTable.setText("运行后显示结果");
        resultTable.setMinHeight(dp(120));
        panel.addView(resultTable, matchWrap());

        timingView = new TextView(this);
        timingView.setTextSize(12);
        timingView.setText("计时：-");
        panel.addView(timingView, matchWrap());

        return panel;
    }

    private LinearLayout createRightPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(6), 0, 0, 0);

        TextView promptLabel = sectionLabel("Prompt");
        panel.addView(promptLabel, matchWrap());

        promptPreview = outputText();
        promptPreview.setMaxHeight(dp(140));
        promptPreview.setText("加载用例后显示");
        panel.addView(promptPreview, matchWrap());

        TextView rawLabel = sectionLabel("Raw Output");
        panel.addView(rawLabel, matchWrap());

        rawOutputView = outputText();
        rawOutputView.setMaxHeight(dp(140));
        rawOutputView.setText("");
        panel.addView(rawOutputView, matchWrap());

        TextView historyLabel = sectionLabel("历史对比");
        panel.addView(historyLabel, matchWrap());

        historyView = outputText();
        historyView.setMaxHeight(dp(100));
        historyView.setText("暂无历史数据");
        panel.addView(historyView, matchWrap());

        return panel;
    }

    // ---- Actions ----

    private void loadBuiltinFixture() {
        try (InputStream imageStream = getAssets().open(BUILTIN_FIXTURE_DIR + "/screenshot.jpg");
             InputStream targetsStream = getAssets().open(BUILTIN_FIXTURE_DIR + "/targets.json")) {
            fixtureBitmap = BitmapFactory.decodeStream(imageStream);
            fixtureTestSet = IconExperimentJson.parseTestSet(readUtf8(targetsStream));
            updateImagePreview();
            updatePromptPreview();
            refreshHistory();
            statusView.setText("状态：已加载 WeLink fixture，"
                    + fixtureTestSet.targets().size() + " 个目标");
        } catch (Exception e) {
            statusView.setText("状态：fixture 加载失败 - " + e.getMessage());
        }
    }

    private void initializeModel() {
        boolean preferGpu = gpuSwitch.isChecked();
        SmallModelInitConfig baseConfig = SmallModelInitConfig.defaultFor(this);
        SmallModelInitConfig config = SmallModelInitConfig.builder()
                .setModelPath(baseConfig.modelPath())
                .setMaxTokens(EXPERIMENT_MAX_TOKENS)
                .setTopK(EXPERIMENT_TOP_K)
                .setTopP(EXPERIMENT_TOP_P)
                .setTemperature(EXPERIMENT_TEMPERATURE)
                .setPreferGpu(preferGpu)
                .build();
        client.close();
        lastLoadLatencyMs = -1L;
        setBusy(true, "状态：模型加载中 (" + (preferGpu ? "GPU" : "CPU") + ")");
        long startedAtMs = System.currentTimeMillis();
        client.initialize(this, config, new SmallModelCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                long latencyMs = System.currentTimeMillis() - startedAtMs;
                runOnUiThread(() -> {
                    lastLoadLatencyMs = latencyMs;
                    setBusy(false, "状态：已加载 " + (preferGpu ? "GPU" : "CPU")
                            + " " + latencyMs + "ms");
                });
            }

            @Override
            public void onError(SmallModelError error) {
                runOnUiThread(() -> setBusy(false,
                        "状态：加载失败 - " + (error == null ? "" : error.message())));
            }
        });
    }

    private void runExperiment() {
        Log.i(TAG, "runExperiment clicked. initialized=" + client.isInitialized()
                + ", fixtureBitmap=" + (fixtureBitmap != null)
                + ", fixtureTestSet=" + (fixtureTestSet != null));
        if (!client.isInitialized()) {
            initializeModel();
            statusView.setText("状态：请等待模型加载后再运行");
            return;
        }
        if (fixtureBitmap == null || fixtureTestSet == null) {
            statusView.setText("状态：fixture 未加载");
            return;
        }

        setBusy(true, "状态：实验运行中");
        resultTable.setText("运行中...");
        rawOutputView.setText("");
        IconInputMode mode = selectedInputMode();
        int maxEdge = selectedMaxEdge();
        Bitmap screenshot = fixtureBitmap;
        IconExperimentTestSet testSet = fixtureTestSet;
        Log.i(TAG, "calling IconExperimentRunner.run. mode=" + mode
                + ", maxEdge=" + maxEdge
                + ", bitmapSize=" + screenshot.getWidth() + "x" + screenshot.getHeight()
                + ", targetCount=" + testSet.targets().size());

        experimentExecutor.execute(() -> {
            dumpDebugTargetCrops(screenshot, testSet);
            IconExperimentRunner.run(
                    screenshot,
                    testSet,
                    mode,
                    lastLoadLatencyMs,
                    client,
                    result -> runOnUiThread(() -> handleResult(result)),
                    maxEdge
            );
        });
    }

    private void handleResult(IconExperimentRunResult result) {
        Log.i(TAG, "handleResult called. result=" + (result != null)
                + ", error=" + (result != null && result.error() != null
                        ? result.error().code() + ":" + result.error().message() : "null"));
        if (result == null) {
            setBusy(false, "状态：结果为空");
            return;
        }

        // Auto-match
        List<IconTarget> targets = result.targets().isEmpty()
                ? fixtureTestSet.targets()
                : result.targets();
        List<IconMatchResult> matches = IconResultMatcher.match(
                result.parsedOutput(), targets);
        resultTable.setText(IconResultMatcher.formatResultTable(matches));

        // Timing
        timingView.setText("计时：图片 " + formatMs(result.imagePrepareMs())
                + "  推理 " + formatMs(result.inferenceMs())
                + "  总计 " + formatMs(result.totalMs()));

        // Raw output
        rawOutputView.setText(result.rawOutput().isEmpty()
                ? (result.error() == null ? "" : result.error().code())
                : result.rawOutput());

        // Save
        String saveInfo = "";
        try {
            java.io.File saved = IconExperimentResultStore.save(this, result);
            saveInfo = "\n保存: " + saved.getName();
        } catch (Exception e) {
            saveInfo = "\n保存失败: " + e.getMessage();
        }

        // Status
        String status = result.error() == null
                ? "状态：完成" + saveInfo
                : "状态：失败 - " + result.error().code() + saveInfo;
        setBusy(false, status);

        refreshHistory();
    }

    private void dumpDebugTargetCrops(Bitmap screenshot, IconExperimentTestSet testSet) {
        if (screenshot == null || testSet == null) {
            return;
        }
        File baseDir = getExternalFilesDir(null);
        if (baseDir == null) {
            baseDir = getFilesDir();
        }
        File outputDir = new File(baseDir, "small-model-experiments/debug-crops/"
                + System.currentTimeMillis() + "_" + testSet.testsetId());
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Log.w(TAG, "failed to create debug crop dir: " + outputDir.getAbsolutePath());
            return;
        }
        Log.i(TAG, "dump debug crops. dir=" + outputDir.getAbsolutePath()
                + ", bitmap=" + screenshot.getWidth() + "x" + screenshot.getHeight()
                + ", targetCount=" + testSet.targets().size());

        Bitmap annotated = createBoundsPreview(screenshot, testSet);
        saveBitmap(new File(outputDir, "annotated.png"), annotated);
        annotated.recycle();

        for (IconTarget target : testSet.targets()) {
            IconBounds bounds = target.bounds();
            if (bounds == null || !bounds.isValid()) {
                Log.w(TAG, "skip invalid target bounds. id=" + target.id());
                continue;
            }
            int left = clamp(bounds.left(), 0, screenshot.getWidth() - 1);
            int top = clamp(bounds.top(), 0, screenshot.getHeight() - 1);
            int right = clamp(bounds.right(), left + 1, screenshot.getWidth());
            int bottom = clamp(bounds.bottom(), top + 1, screenshot.getHeight());
            Bitmap crop = Bitmap.createBitmap(screenshot, left, top, right - left, bottom - top);
            File file = new File(outputDir, target.id() + "_"
                    + left + "_" + top + "_" + right + "_" + bottom + ".png");
            saveBitmap(file, crop);
            crop.recycle();
            Log.i(TAG, "debug crop saved. id=" + target.id()
                    + ", raw=" + bounds.left() + "," + bounds.top() + ","
                    + bounds.right() + "," + bounds.bottom()
                    + ", clamped=" + left + "," + top + "," + right + "," + bottom
                    + ", crop=" + (right - left) + "x" + (bottom - top)
                    + ", file=" + file.getAbsolutePath());
        }
    }

    private void saveBitmap(File file, Bitmap bitmap) {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (Exception e) {
            Log.e(TAG, "save debug bitmap failed. file=" + file.getAbsolutePath(), e);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void refreshHistory() {
        try {
            List<IconExperimentRunResult> history =
                    IconExperimentResultStore.listResults(this);
            historyView.setText(
                    IconExperimentResultStore.formatHistorySummary(history, 10));
        } catch (Exception e) {
            historyView.setText("历史读取失败");
        }
    }

    // ---- UI helpers ----

    private void updateImagePreview() {
        if (imagePreview == null || fixtureBitmap == null) {
            return;
        }
        IconInputMode mode = selectedInputMode();
        if ((mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED)
                && fixtureTestSet != null) {
            imagePreview.setImageBitmap(createBoundsPreview(fixtureBitmap, fixtureTestSet));
        } else if (mode == IconInputMode.CROPPED_MONTAGE && fixtureTestSet != null) {
            imagePreview.setImageBitmap(fixtureBitmap);
        } else {
            imagePreview.setImageBitmap(fixtureBitmap);
        }
    }

    private void updatePromptPreview() {
        if (promptPreview == null || fixtureTestSet == null) {
            return;
        }
        IconInputMode mode = selectedInputMode();
        String prompt;
        if (mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED) {
            if (mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS) {
                prompt = IconExperimentPromptBuilder.markedBoundsPrompt(fixtureTestSet);
            } else {
                prompt = IconExperimentPromptBuilder.fullImageWithBoundsPrompt(
                        fixtureTestSet,
                        fixtureBitmap == null ? 0 : fixtureBitmap.getWidth(),
                        fixtureBitmap == null ? 0 : fixtureBitmap.getHeight()
                );
            }
        } else if (mode == IconInputMode.CROPPED_MONTAGE) {
            prompt = IconExperimentPromptBuilder.montagePrompt(
                    IconMontageLayoutCalculator.calculate(fixtureTestSet.targets()).mappings()
            );
        } else {
            prompt = IconExperimentPromptBuilder.fullImagePrompt(fixtureTestSet);
        }
        promptPreview.setText(prompt);
    }

    private Bitmap createBoundsPreview(Bitmap source, IconExperimentTestSet testSet) {
        Bitmap preview = Bitmap.createBitmap(
                source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(preview);
        canvas.drawBitmap(source, 0f, 0f, null);
        float strokeWidth = Math.max(3f, source.getWidth() / 320f);
        float labelTextSize = Math.max(18f, source.getWidth() / 58f);
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(Color.rgb(0, 120, 255));
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xCC0078FF);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(labelTextSize);
        for (IconTarget target : testSet.targets()) {
            IconBounds bounds = target.bounds();
            if (bounds == null || !bounds.isValid()) {
                continue;
            }
            canvas.drawRect(bounds.left(), bounds.top(),
                    bounds.right(), bounds.bottom(), strokePaint);
            float padding = Math.max(4f, canvas.getWidth() / 310f);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float tw = textPaint.measureText(target.id());
            float th = metrics.descent - metrics.ascent;
            float left = bounds.left();
            float top = Math.max(0f, bounds.top() - th - padding * 2f);
            float right = Math.min(canvas.getWidth(), left + tw + padding * 2f);
            float bottom = top + th + padding * 2f;
            canvas.drawRect(left, top, right, bottom, bgPaint);
            canvas.drawText(target.id(), left + padding, bottom - padding - metrics.descent,
                    textPaint);
        }
        return preview;
    }

    private void setBusy(boolean busy, String status) {
        statusView.setText(status);
        initButton.setEnabled(!busy);
        runButton.setEnabled(!busy);
        gpuSwitch.setEnabled(!busy);
        inputModeSpinner.setEnabled(!busy);
        maxEdgeSpinner.setEnabled(!busy);
    }

    private IconInputMode selectedInputMode() {
        String value = (String) inputModeSpinner.getSelectedItem();
        try {
            return IconInputMode.valueOf(value);
        } catch (Exception ignored) {
            return IconInputMode.FULL_IMAGE;
        }
    }

    private int selectedMaxEdge() {
        String value = (String) maxEdgeSpinner.getSelectedItem();
        if ("原图".equals(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatMs(long ms) {
        return ms >= 0 ? ms + "ms" : "-";
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView sectionLabel(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(0xFF666666);
        view.setPadding(0, dp(6), 0, dp(2));
        return view;
    }

    private TextView outputText() {
        TextView view = new TextView(this);
        view.setTextSize(12);
        view.setTextIsSelectable(true);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        view.setBackgroundColor(0xFFF1F3F4);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String readUtf8(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
