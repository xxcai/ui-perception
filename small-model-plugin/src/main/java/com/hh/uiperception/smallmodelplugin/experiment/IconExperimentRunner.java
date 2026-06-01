package com.hh.uiperception.smallmodelplugin.experiment;

import android.graphics.Bitmap;
import android.util.Log;

import com.hh.uiperception.smallmodelplugin.api.SmallModelCallback;
import com.hh.uiperception.smallmodelplugin.api.SmallModelError;
import com.hh.uiperception.smallmodelplugin.api.SmallModelRequest;
import com.hh.uiperception.smallmodelplugin.api.SmallModelResult;
import com.hh.uiperception.smallmodelplugin.api.SmallModelVisionClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 串联输入生成、模型调用和结构化结果构造。
 */
public final class IconExperimentRunner {

    private static final String TAG = "IconExperimentRunner";

    private IconExperimentRunner() {
    }

    public static void run(
            Bitmap screenshot,
            IconExperimentTestSet testSet,
            IconInputMode inputMode,
            long modelLoadMs,
            SmallModelVisionClient client,
            IconExperimentRunCallback callback,
            int imageMaxEdge,
            String imageEncoding,
            String bitmapConfig
    ) {
        long createdAtMs = System.currentTimeMillis();
        long startedAtMs = createdAtMs;
        IconInputMode mode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        String runId = IconExperimentRunId.create(mode, createdAtMs);
        Log.i(TAG, "run started. mode=" + mode + ", runId=" + runId
                + ", thread=" + Thread.currentThread().getName()
                + ", screenshot=" + (screenshot != null ? screenshot.getWidth() + "x" + screenshot.getHeight() : "null")
                + ", targetCount=" + (testSet == null ? 0 : testSet.targets().size())
                + ", client=" + client + ", clientInitialized=" + (client != null && client.isInitialized())
                + ", imageMaxEdge=" + imageMaxEdge
                + ", imageEncoding=" + normalizeImageEncoding(imageEncoding)
                + ", bitmapConfig=" + normalizeBitmapConfig(bitmapConfig));

        if (mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED) {
            runBatchedFullImageWithBounds(
                    screenshot, testSet, 3, modelLoadMs, client, callback,
                    createdAtMs, startedAtMs, runId, imageMaxEdge,
                    normalizeImageEncoding(imageEncoding),
                    normalizeBitmapConfig(bitmapConfig)
            );
            return;
        }

        IconExperimentInput input;
        try {
            input = IconExperimentInputBuilder.build(screenshot, testSet, mode);
            Log.i(TAG, "input built. mode=" + input.inputMode()
                    + ", image=" + (input.image() != null ? input.image().getWidth() + "x" + input.image().getHeight() : "null")
                    + ", promptLength=" + (input.prompt() != null ? input.prompt().length() : 0)
                    + ", imagePrepareMs=" + input.imagePrepareMs());
        } catch (Throwable throwable) {
            Log.e(TAG, "input build failed", throwable);
            dispatch(callback, failureResult(
                    runId, createdAtMs, testSet, mode, modelLoadMs,
                    -1L, System.currentTimeMillis() - startedAtMs,
                    0, 1, "", "",
                    new IconExperimentError("INPUT_PREPARE_FAILED", throwable.getMessage())
            ));
            return;
        }
        if (client == null || !client.isInitialized()) {
            Log.e(TAG, "client not initialized. client=" + client);
            dispatch(callback, failureResult(
                    runId, createdAtMs, testSet, mode, modelLoadMs,
                    input.imagePrepareMs(), System.currentTimeMillis() - startedAtMs,
                    0, 1, input.prompt(), "",
                    new IconExperimentError(SmallModelError.CODE_NOT_INITIALIZED, "小模型尚未初始化")
            ));
            return;
        }

        SmallModelRequest.Builder requestBuilder = SmallModelRequest.builder()
                .setImage(input.image())
                .setPrompt(input.prompt())
                .setBaselineId(testSet == null ? "" : testSet.testsetId())
                .putOption("icon_experiment_run_id", runId)
                .putOption("icon_experiment_input_mode", mode.name());
        if (imageMaxEdge > 0) {
            requestBuilder.putOption(
                    SmallModelRequest.OPTION_IMAGE_MAX_EDGE,
                    String.valueOf(imageMaxEdge));
        }
        requestBuilder.putOption(
                SmallModelRequest.OPTION_IMAGE_ENCODING,
                normalizeImageEncoding(imageEncoding));
        requestBuilder.putOption(
                SmallModelRequest.OPTION_BITMAP_CONFIG,
                normalizeBitmapConfig(bitmapConfig));
        SmallModelRequest request = requestBuilder.build();
        Log.i(TAG, "calling client.analyze. promptLength=" + input.prompt().length()
                + ", image=" + (input.image() != null ? input.image().getWidth() + "x" + input.image().getHeight() : "null"));

        client.analyze(request, new SmallModelCallback<SmallModelResult>() {
            @Override
            public void onSuccess(SmallModelResult value) {
                Log.i(TAG, "client.analyze onSuccess. rawTextLength="
                        + (value != null ? value.rawText().length() : -1)
                        + ", latencyMs=" + (value != null ? value.latencyMs() : -1)
                        + ", thread=" + Thread.currentThread().getName());
                String rawOutput = value == null ? "" : value.rawText();
                long inferenceMs = value == null ? -1L : value.latencyMs();
                dispatch(callback, IconExperimentRunResult.builder()
                        .setRunId(runId)
                        .setCreatedAtMs(createdAtMs)
                        .setTestsetId(testSet == null ? "" : testSet.testsetId())
                        .setImage(testSet == null ? "" : testSet.image())
                        .setInputMode(mode)
                        .setTargetCount(targetCount(testSet))
                        .setImageMaxEdge(imageMaxEdge)
                        .setImageEncoding(normalizeImageEncoding(imageEncoding))
                        .setBitmapConfig(normalizeBitmapConfig(bitmapConfig))
                        .setImagePrepareMs(input.imagePrepareMs())
                        .setInputImageWidth(value == null ? 0 : value.inputWidth())
                        .setInputImageHeight(value == null ? 0 : value.inputHeight())
                        .setEncodedImageWidth(value == null ? 0 : value.encodedWidth())
                        .setEncodedImageHeight(value == null ? 0 : value.encodedHeight())
                        .setEncodedImageBytes(value == null ? 0 : value.imageBytes())
                        .setImageEncodeMs(value == null ? -1L : value.imageEncodeMs())
                        .setModelCallMs(value == null ? -1L : value.modelCallMs())
                        .setModelLoadMs(modelLoadMs)
                        .setInferenceMs(inferenceMs)
                        .setTotalMs(System.currentTimeMillis() - startedAtMs)
                        .setBatchSize(0)
                        .setBatchCount(1)
                        .setPrompt(input.prompt())
                        .setTargets(testSet == null ? null : testSet.targets())
                        .setRawOutput(rawOutput)
                        .setParsedOutput(IconOutputParser.parse(rawOutput, input.mappings()))
                        .setManualScores(defaultManualScores(testSet))
                        .build());
            }

            @Override
            public void onError(SmallModelError error) {
                Log.e(TAG, "client.analyze onError. code="
                        + (error != null ? error.code() : "null")
                        + ", message=" + (error != null ? error.message() : "null")
                        + ", thread=" + Thread.currentThread().getName());
                dispatch(callback, failureResult(
                        runId, createdAtMs, testSet, mode, modelLoadMs,
                        input.imagePrepareMs(), System.currentTimeMillis() - startedAtMs,
                        0, 1, input.prompt(), "",
                        new IconExperimentError(
                                error == null ? SmallModelError.CODE_INFERENCE_FAILED : error.code(),
                                error == null ? "" : error.message()
                        )
                ));
            }
        });
    }

    private static void runBatchedFullImageWithBounds(
            Bitmap screenshot,
            IconExperimentTestSet testSet,
            int batchSize,
            long modelLoadMs,
            SmallModelVisionClient client,
            IconExperimentRunCallback callback,
            long createdAtMs,
            long startedAtMs,
            String runId,
            int imageMaxEdge,
            String imageEncoding,
            String bitmapConfig
    ) {
        if (screenshot == null) {
            dispatch(callback, failureResult(
                    runId, createdAtMs, testSet,
                    IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED, modelLoadMs,
                    -1L, System.currentTimeMillis() - startedAtMs,
                    0, 1, "", "",
                    new IconExperimentError("INPUT_PREPARE_FAILED", "screenshot must not be null")
            ));
            return;
        }
        if (client == null || !client.isInitialized()) {
            dispatch(callback, failureResult(
                    runId, createdAtMs, testSet,
                    IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED, modelLoadMs,
                    -1L, System.currentTimeMillis() - startedAtMs,
                    Math.max(1, batchSize), 1, "", "",
                    new IconExperimentError(SmallModelError.CODE_NOT_INITIALIZED, "小模型尚未初始化")
            ));
            return;
        }
        List<IconTarget> targets = testSet == null ? new ArrayList<>() : testSet.targets();
        int safeBatchSize = normalizeBatchSize(batchSize);
        int batchCount = targets.isEmpty()
                ? 0
                : (int) Math.ceil(targets.size() / (double) safeBatchSize);
        List<IconTargetMapping> mappings = IconExperimentInputBuilder.buildMappings(testSet);
        BatchState state = new BatchState(
                screenshot, testSet, targets, mappings, safeBatchSize, batchCount,
                modelLoadMs, client, callback,
                createdAtMs, startedAtMs, runId, imageMaxEdge,
                normalizeImageEncoding(imageEncoding),
                normalizeBitmapConfig(bitmapConfig)
        );
        runNextBatch(state);
    }

    private static void runNextBatch(BatchState state) {
        Log.i(TAG, "runNextBatch. nextIndex=" + state.nextIndex
                + ", total=" + state.targets.size()
                + ", thread=" + Thread.currentThread().getName());
        if (state.nextIndex >= state.targets.size()) {
            dispatch(state.callback, IconExperimentRunResult.builder()
                    .setRunId(state.runId)
                    .setCreatedAtMs(state.createdAtMs)
                    .setTestsetId(state.testSet == null ? "" : state.testSet.testsetId())
                    .setImage(state.testSet == null ? "" : state.testSet.image())
                    .setInputMode(IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED)
                    .setTargetCount(targetCount(state.testSet))
                    .setImageMaxEdge(state.imageMaxEdge)
                    .setImageEncoding(state.imageEncoding)
                    .setBitmapConfig(state.bitmapConfig)
                    .setImagePrepareMs(state.imagePrepareMs)
                    .setInputImageWidth(state.inputImageWidth)
                    .setInputImageHeight(state.inputImageHeight)
                    .setEncodedImageWidth(state.encodedImageWidth)
                    .setEncodedImageHeight(state.encodedImageHeight)
                    .setEncodedImageBytes(state.encodedImageBytes)
                    .setImageEncodeMs(state.imageEncodeMs)
                    .setModelCallMs(state.modelCallMs)
                    .setModelLoadMs(state.modelLoadMs)
                    .setInferenceMs(state.inferenceMs)
                    .setTotalMs(System.currentTimeMillis() - state.startedAtMs)
                    .setBatchSize(state.batchSize)
                    .setBatchCount(Math.max(1, state.batchCount))
                    .setPrompt(state.prompts.toString().trim())
                    .setTargets(state.testSet == null ? null : state.testSet.targets())
                    .setRawOutput(state.rawOutput.toString().trim())
                    .setParsedOutput(IconOutputParser.parse(state.rawOutput.toString(), state.mappings))
                    .setManualScores(defaultManualScores(state.testSet))
                    .build());
            return;
        }

        int batchStart = state.nextIndex;
        int batchEnd = Math.min(state.targets.size(), batchStart + state.batchSize);
        List<IconTargetMapping> batchMappings = new ArrayList<>(
                state.mappings.subList(batchStart, batchEnd)
        );
        List<IconTarget> batchTargets = new ArrayList<>(
                state.targets.subList(batchStart, batchEnd)
        );
        IconExperimentTestSet batchTestSet = new IconExperimentTestSet(
                state.testSet == null ? "" : state.testSet.testsetId(),
                state.testSet == null ? "" : state.testSet.image(),
                batchTargets
        );
        long prepareStartedAtMs = System.currentTimeMillis();
        String prompt = IconExperimentPromptBuilder.fullImageWithBoundsPrompt(
                batchMappings,
                state.screenshot.getWidth(),
                state.screenshot.getHeight()
        );
        state.imagePrepareMs += System.currentTimeMillis() - prepareStartedAtMs;
        int batchNumber = batchStart / state.batchSize + 1;
        state.prompts.append("Batch ")
                .append(batchNumber)
                .append("/")
                .append(Math.max(1, state.batchCount))
                .append("\n")
                .append(prompt)
                .append("\n\n");

        SmallModelRequest.Builder requestBuilder = SmallModelRequest.builder()
                .setImage(state.screenshot)
                .setPrompt(prompt)
                .setBaselineId(state.testSet == null ? "" : state.testSet.testsetId())
                .putOption("icon_experiment_run_id", state.runId)
                .putOption("icon_experiment_input_mode", IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED.name())
                .putOption("icon_experiment_batch_number", String.valueOf(batchNumber))
                .putOption("icon_experiment_batch_size", String.valueOf(state.batchSize));
        if (state.imageMaxEdge > 0) {
            requestBuilder.putOption(
                    SmallModelRequest.OPTION_IMAGE_MAX_EDGE,
                    String.valueOf(state.imageMaxEdge));
        }
        requestBuilder.putOption(
                SmallModelRequest.OPTION_IMAGE_ENCODING,
                state.imageEncoding);
        requestBuilder.putOption(
                SmallModelRequest.OPTION_BITMAP_CONFIG,
                state.bitmapConfig);
        SmallModelRequest request = requestBuilder.build();

        state.client.analyze(request, new SmallModelCallback<SmallModelResult>() {
            @Override
            public void onSuccess(SmallModelResult value) {
                String rawOutput = value == null ? "" : value.rawText();
                long latencyMs = value == null ? -1L : value.latencyMs();
                if (latencyMs >= 0L) {
                    state.inferenceMs += latencyMs;
                }
                if (state.inputImageWidth <= 0 && value != null) {
                    state.inputImageWidth = value.inputWidth();
                    state.inputImageHeight = value.inputHeight();
                    state.encodedImageWidth = value.encodedWidth();
                    state.encodedImageHeight = value.encodedHeight();
                    state.encodedImageBytes = value.imageBytes();
                    state.imageEncodeMs = value.imageEncodeMs();
                    state.modelCallMs = value.modelCallMs();
                }
                if (state.rawOutput.length() > 0) {
                    state.rawOutput.append("\n");
                }
                state.rawOutput.append(rawOutput);
                state.nextIndex = batchEnd;
                runNextBatch(state);
            }

            @Override
            public void onError(SmallModelError error) {
                dispatch(state.callback, failureResult(
                        state.runId, state.createdAtMs, state.testSet,
                        IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED, state.modelLoadMs,
                        state.imagePrepareMs, System.currentTimeMillis() - state.startedAtMs,
                        state.batchSize, Math.max(1, state.batchCount),
                        state.prompts.toString().trim(), state.rawOutput.toString().trim(),
                        new IconExperimentError(
                                error == null ? SmallModelError.CODE_INFERENCE_FAILED : error.code(),
                                error == null ? "" : error.message()
                        )
                ));
            }
        });
    }

    private static int normalizeBatchSize(int batchSize) {
        if (batchSize >= 3 && batchSize <= 5) {
            return batchSize;
        }
        return 3;
    }

    private static String normalizeImageEncoding(String imageEncoding) {
        if (imageEncoding == null || imageEncoding.trim().isEmpty()) {
            return SmallModelRequest.OPTION_IMAGE_ENCODING_PNG;
        }
        return imageEncoding.trim();
    }

    private static String normalizeBitmapConfig(String bitmapConfig) {
        if (SmallModelRequest.OPTION_BITMAP_CONFIG_RGB_565.equals(bitmapConfig)) {
            return SmallModelRequest.OPTION_BITMAP_CONFIG_RGB_565;
        }
        return SmallModelRequest.OPTION_BITMAP_CONFIG_ARGB_8888;
    }

    private static IconExperimentRunResult failureResult(
            String runId,
            long createdAtMs,
            IconExperimentTestSet testSet,
            IconInputMode inputMode,
            long modelLoadMs,
            long imagePrepareMs,
            long totalMs,
            int batchSize,
            int batchCount,
            String prompt,
            String rawOutput,
            IconExperimentError error
    ) {
        return IconExperimentRunResult.builder()
                .setRunId(runId)
                .setCreatedAtMs(createdAtMs)
                .setTestsetId(testSet == null ? "" : testSet.testsetId())
                .setImage(testSet == null ? "" : testSet.image())
                .setInputMode(inputMode)
                .setTargetCount(targetCount(testSet))
                .setImagePrepareMs(imagePrepareMs)
                .setModelLoadMs(modelLoadMs)
                .setInferenceMs(-1L)
                .setTotalMs(totalMs)
                .setBatchSize(batchSize)
                .setBatchCount(batchCount)
                .setPrompt(prompt)
                .setTargets(testSet == null ? null : testSet.targets())
                .setRawOutput(rawOutput)
                .setParsedOutput(IconOutputParser.parse(rawOutput))
                .setManualScores(defaultManualScores(testSet))
                .setError(error)
                .build();
    }

    private static int targetCount(IconExperimentTestSet testSet) {
        return testSet == null ? 0 : testSet.targets().size();
    }

    private static List<IconManualScore> defaultManualScores(IconExperimentTestSet testSet) {
        List<IconManualScore> scores = new ArrayList<>();
        if (testSet == null) {
            return scores;
        }
        for (IconTarget target : testSet.targets()) {
            scores.add(new IconManualScore(target.id(), null, ""));
        }
        return scores;
    }

    private static void dispatch(IconExperimentRunCallback callback, IconExperimentRunResult result) {
        if (callback != null) {
            callback.onComplete(result);
        }
    }

    private static final class BatchState {
        private final Bitmap screenshot;
        private final IconExperimentTestSet testSet;
        private final List<IconTarget> targets;
        private final List<IconTargetMapping> mappings;
        private final int batchSize;
        private final int batchCount;
        private final long modelLoadMs;
        private final SmallModelVisionClient client;
        private final IconExperimentRunCallback callback;
        private final long createdAtMs;
        private final long startedAtMs;
        private final String runId;
        private final int imageMaxEdge;
        private final String imageEncoding;
        private final String bitmapConfig;
        private final StringBuilder prompts = new StringBuilder();
        private final StringBuilder rawOutput = new StringBuilder();
        private int nextIndex;
        private long imagePrepareMs;
        private long inferenceMs;
        private int inputImageWidth;
        private int inputImageHeight;
        private int encodedImageWidth;
        private int encodedImageHeight;
        private int encodedImageBytes;
        private long imageEncodeMs;
        private long modelCallMs;

        private BatchState(
                Bitmap screenshot,
                IconExperimentTestSet testSet,
                List<IconTarget> targets,
                List<IconTargetMapping> mappings,
                int batchSize,
                int batchCount,
                long modelLoadMs,
                SmallModelVisionClient client,
                IconExperimentRunCallback callback,
                long createdAtMs,
                long startedAtMs,
                String runId,
                int imageMaxEdge,
                String imageEncoding,
                String bitmapConfig
        ) {
            this.screenshot = screenshot;
            this.testSet = testSet;
            this.targets = targets == null ? new ArrayList<>() : targets;
            this.mappings = mappings == null ? new ArrayList<>() : mappings;
            this.batchSize = batchSize;
            this.batchCount = batchCount;
            this.modelLoadMs = modelLoadMs;
            this.client = client;
            this.callback = callback;
            this.createdAtMs = createdAtMs;
            this.startedAtMs = startedAtMs;
            this.runId = runId;
            this.imageMaxEdge = imageMaxEdge;
            this.imageEncoding = normalizeImageEncoding(imageEncoding);
            this.bitmapConfig = normalizeBitmapConfig(bitmapConfig);
        }
    }
}
