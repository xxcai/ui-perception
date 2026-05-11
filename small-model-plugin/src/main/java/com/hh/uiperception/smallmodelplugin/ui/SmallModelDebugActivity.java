package com.hh.uiperception.smallmodelplugin.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.hh.uiperception.smallmodelplugin.api.SmallModelCallback;
import com.hh.uiperception.smallmodelplugin.api.SmallModelError;
import com.hh.uiperception.smallmodelplugin.api.SmallModelInitConfig;
import com.hh.uiperception.smallmodelplugin.api.SmallModelRequest;
import com.hh.uiperception.smallmodelplugin.api.SmallModelResult;
import com.hh.uiperception.smallmodelplugin.gemma.Gemma4E2BClient;
import com.hh.uiperception.smallmodelplugin.gemma.GemmaUiUnderstandingPrompt;

import java.io.InputStream;

/**
 * Gemma 小模型调试页。
 */
public final class SmallModelDebugActivity extends Activity {

    private static final int REQUEST_PICK_IMAGE = 1001;

    private final Gemma4E2BClient client = new Gemma4E2BClient();

    private TextView statusView;
    private TextView timingView;
    private TextView experimentView;
    private TextView modelPathView;
    private EditText promptInput;
    private ImageView imagePreview;
    private TextView rawOutputView;
    private TextView yamlOutputView;
    private Switch backendSwitch;
    private Spinner promptModeSpinner;
    private Button initButton;
    private Button analyzeButton;

    private Bitmap selectedBitmap;
    private long lastLoadLatencyMs = -1L;
    private long lastInferenceLatencyMs = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Gemma 小模型调试");
        setContentView(createContentView());
        updateModelPath();
    }

    @Override
    protected void onDestroy() {
        client.close();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadImage(uri);
            }
        }
    }

    private ScrollView createContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(24));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("Gemma-4-E2B-it 调试");
        title.setTextSize(24);
        title.setGravity(Gravity.START);
        content.addView(title);

        statusView = label("状态：未初始化");
        content.addView(statusView, matchWrap());

        timingView = label("计时：加载 - / 识别 -");
        content.addView(timingView, matchWrap());

        experimentView = label("");
        content.addView(experimentView, matchWrap());
        updateExperimentConfig();

        modelPathView = label("");
        content.addView(modelPathView, matchWrap());

        backendSwitch = new Switch(this);
        backendSwitch.setText("使用 GPU 后端");
        backendSwitch.setChecked(true);
        backendSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateExperimentConfig());
        content.addView(backendSwitch, matchWrap());

        initButton = new Button(this);
        initButton.setText("加载模型");
        initButton.setOnClickListener(v -> initializeModel());
        content.addView(initButton, matchWrap());

        Button pickImageButton = new Button(this);
        pickImageButton.setText("选择图片");
        pickImageButton.setOnClickListener(v -> pickImage());
        content.addView(pickImageButton, matchWrap());

        imagePreview = new ImageView(this);
        imagePreview.setAdjustViewBounds(true);
        imagePreview.setMinimumHeight(dp(160));
        content.addView(imagePreview, matchWrap());

        promptInput = new EditText(this);
        promptInput.setMinLines(6);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        promptInput.setText(GemmaUiUnderstandingPrompt.defaultPrompt());
        content.addView(promptInput, matchWrap());

        promptModeSpinner = spinner(new String[]{
                "完整",
                "精简",
                "极简",
                "Ref说明 n1+n2",
                "Ref说明 n1",
                "Ref说明 n2",
                "Ref说明 顶部图标"
        });
        promptModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                promptInput.setText(promptForMode(promptMode()));
                updateExperimentConfig();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        content.addView(formRow("Prompt 模式", promptModeSpinner), matchWrap());

        analyzeButton = new Button(this);
        analyzeButton.setText("运行识图");
        analyzeButton.setOnClickListener(v -> analyzeImage());
        content.addView(analyzeButton, matchWrap());

        TextView rawTitle = sectionTitle("Raw Output");
        content.addView(rawTitle, matchWrap());
        rawOutputView = outputText();
        content.addView(rawOutputView, matchWrap());

        TextView yamlTitle = sectionTitle("Normalized YAML");
        content.addView(yamlTitle, matchWrap());
        yamlOutputView = outputText();
        content.addView(yamlOutputView, matchWrap());

        return scrollView;
    }

    private void updateModelPath() {
        SmallModelInitConfig config = SmallModelInitConfig.defaultFor(this);
        modelPathView.setText("模型路径：\n" + config.modelPath());
    }

    private void initializeModel() {
        boolean preferGpu = backendSwitch.isChecked();
        SmallModelInitConfig baseConfig = SmallModelInitConfig.defaultFor(this);
        SmallModelInitConfig config = SmallModelInitConfig.builder()
                .setModelPath(baseConfig.modelPath())
                .setMaxTokens(baseConfig.maxTokens())
                .setTopK(baseConfig.topK())
                .setTopP(baseConfig.topP())
                .setTemperature(baseConfig.temperature())
                .setPreferGpu(preferGpu)
                .build();
        client.close();
        rawOutputView.setText("");
        yamlOutputView.setText("");
        lastLoadLatencyMs = -1L;
        lastInferenceLatencyMs = -1L;
        updateTiming();
        updateExperimentConfig();
        long startedAtMs = System.currentTimeMillis();
        setBusy(true, "状态：模型加载中，后端：" + backendName(preferGpu));
        client.initialize(this, config, new SmallModelCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                long latencyMs = System.currentTimeMillis() - startedAtMs;
                runOnUiThread(() -> {
                    lastLoadLatencyMs = latencyMs;
                    updateTiming();
                    setBusy(false, "状态：模型已加载，后端：" + backendName(preferGpu)
                            + "，加载耗时 " + latencyMs + " ms");
                });
            }

            @Override
            public void onError(SmallModelError error) {
                long latencyMs = System.currentTimeMillis() - startedAtMs;
                runOnUiThread(() -> {
                    lastLoadLatencyMs = latencyMs;
                    updateTiming();
                    setBusy(false, "状态：加载失败，耗时 " + latencyMs + " ms\n" + error);
                    rawOutputView.setText(error.toString());
                });
            }
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void loadImage(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            selectedBitmap = BitmapFactory.decodeStream(inputStream);
            imagePreview.setImageBitmap(selectedBitmap);
            statusView.setText("状态：已选择图片");
        } catch (Exception e) {
            statusView.setText("状态：图片读取失败\n" + e.getMessage());
        }
    }

    private void analyzeImage() {
        if (!client.isInitialized()) {
            initializeModel();
            statusView.setText("状态：请等待模型加载完成后再次运行识图");
            return;
        }
        if (selectedBitmap == null) {
            statusView.setText("状态：请先选择图片");
            return;
        }

        setBusy(true, "状态：推理中");
        rawOutputView.setText("");
        yamlOutputView.setText("");
        lastInferenceLatencyMs = -1L;
        updateTiming();
        updateExperimentConfig();
        long startedAtMs = System.currentTimeMillis();
        SmallModelRequest request = SmallModelRequest.builder()
                .setImage(selectedBitmap)
                .setPrompt(promptInput.getText().toString())
                .putOption(SmallModelRequest.OPTION_IMAGE_CROP, SmallModelRequest.OPTION_IMAGE_CROP_FULL)
                .putOption(SmallModelRequest.OPTION_IMAGE_MAX_EDGE, "")
                .putOption(SmallModelRequest.OPTION_IMAGE_ENCODING, SmallModelRequest.OPTION_IMAGE_ENCODING_PNG)
                .putOption("prompt_mode", promptMode())
                .putOption("android_id", Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                ))
                .build();
        client.analyze(request, new SmallModelCallback<SmallModelResult>() {
            @Override
            public void onSuccess(SmallModelResult value) {
                runOnUiThread(() -> {
                    long wallLatencyMs = System.currentTimeMillis() - startedAtMs;
                    lastInferenceLatencyMs = value.latencyMs();
                    updateTiming();
                    setBusy(false, "状态：推理完成，识别耗时 " + value.latencyMs()
                            + " ms，端到端耗时 " + wallLatencyMs + " ms");
                    rawOutputView.setText(value.rawText());
                    yamlOutputView.setText(value.normalizedYaml());
                });
            }

            @Override
            public void onError(SmallModelError error) {
                long latencyMs = System.currentTimeMillis() - startedAtMs;
                runOnUiThread(() -> {
                    lastInferenceLatencyMs = latencyMs;
                    updateTiming();
                    setBusy(false, "状态：推理失败，耗时 " + latencyMs + " ms\n" + error);
                    rawOutputView.setText(error.toString());
                });
            }
        });
    }

    private void setBusy(boolean busy, String status) {
        statusView.setText(status);
        initButton.setEnabled(!busy);
        analyzeButton.setEnabled(!busy);
        backendSwitch.setEnabled(!busy);
        promptModeSpinner.setEnabled(!busy);
    }

    private String backendName(boolean preferGpu) {
        return preferGpu ? "GPU" : "CPU";
    }

    private void updateTiming() {
        if (timingView == null) {
            return;
        }
        timingView.setText("计时：加载 " + formatLatency(lastLoadLatencyMs)
                + " / 识别 " + formatLatency(lastInferenceLatencyMs));
    }

    private String formatLatency(long latencyMs) {
        return latencyMs >= 0L ? latencyMs + " ms" : "-";
    }

    private void updateExperimentConfig() {
        if (experimentView == null) {
            return;
        }
        experimentView.setText("配置：Backend=" + backendName(backendSwitch == null || backendSwitch.isChecked())
                + " / MaxTokens=" + SmallModelInitConfig.DEFAULT_MAX_TOKENS
                + " / 图片长边=原图"
                + " / 图片编码=PNG"
                + " / Prompt=" + selectedSpinnerValue(promptModeSpinner, "完整"));
    }

    private String promptMode() {
        return selectedSpinnerValue(promptModeSpinner, "完整");
    }

    private String promptForMode(String mode) {
        if ("精简".equals(mode)) {
            return GemmaUiUnderstandingPrompt.compactPrompt();
        }
        if ("极简".equals(mode)) {
            return GemmaUiUnderstandingPrompt.minimalPrompt();
        }
        if ("Ref说明 n1+n2".equals(mode)) {
            return GemmaUiUnderstandingPrompt.refDescriptionPromptAll();
        }
        if ("Ref说明 n1".equals(mode)) {
            return GemmaUiUnderstandingPrompt.refDescriptionPromptN1();
        }
        if ("Ref说明 n2".equals(mode)) {
            return GemmaUiUnderstandingPrompt.refDescriptionPromptN2();
        }
        if ("Ref说明 顶部图标".equals(mode)) {
            return GemmaUiUnderstandingPrompt.refDescriptionPromptTopIcons();
        }
        return GemmaUiUnderstandingPrompt.defaultPrompt();
    }

    private String selectedSpinnerValue(Spinner spinner, String fallback) {
        if (spinner == null || spinner.getSelectedItem() == null) {
            return fallback;
        }
        return spinner.getSelectedItem().toString();
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setPadding(0, dp(10), 0, dp(8));
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text);
        view.setTextSize(18);
        return view;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view,
                                       int position, long id) {
                updateExperimentConfig();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return spinner;
    }

    private LinearLayout formRow(String label, Spinner spinner) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = label(label);
        row.addView(labelView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(spinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private TextView outputText() {
        TextView view = new TextView(this);
        view.setTextSize(13);
        view.setTextIsSelectable(true);
        view.setPadding(dp(10), dp(10), dp(10), dp(10));
        view.setBackgroundColor(0xFFF1F3F4);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
