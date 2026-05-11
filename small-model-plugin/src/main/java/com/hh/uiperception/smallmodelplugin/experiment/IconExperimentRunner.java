package com.hh.uiperception.smallmodelplugin.experiment;

import android.graphics.Bitmap;

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

    private IconExperimentRunner() {
    }

    public static void run(
            Bitmap screenshot,
            IconExperimentTestSet testSet,
            IconInputMode inputMode,
            long modelLoadMs,
            SmallModelVisionClient client,
            IconExperimentRunCallback callback
    ) {
        long createdAtMs = System.currentTimeMillis();
        long startedAtMs = createdAtMs;
        IconInputMode mode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        String runId = IconExperimentRunId.create(mode, createdAtMs);
        IconExperimentInput input;
        try {
            input = IconExperimentInputBuilder.build(screenshot, testSet, mode);
        } catch (Throwable throwable) {
            dispatch(callback, failureResult(
                    runId,
                    createdAtMs,
                    testSet,
                    mode,
                    modelLoadMs,
                    -1L,
                    System.currentTimeMillis() - startedAtMs,
                    "",
                    "",
                    new IconExperimentError("INPUT_PREPARE_FAILED", throwable.getMessage())
            ));
            return;
        }
        if (client == null || !client.isInitialized()) {
            dispatch(callback, failureResult(
                    runId,
                    createdAtMs,
                    testSet,
                    mode,
                    modelLoadMs,
                    input.imagePrepareMs(),
                    System.currentTimeMillis() - startedAtMs,
                    input.prompt(),
                    "",
                    new IconExperimentError(SmallModelError.CODE_NOT_INITIALIZED, "小模型尚未初始化")
            ));
            return;
        }

        SmallModelRequest request = SmallModelRequest.builder()
                .setImage(input.image())
                .setPrompt(input.prompt())
                .setBaselineId(testSet == null ? "" : testSet.testsetId())
                .putOption("icon_experiment_run_id", runId)
                .putOption("icon_experiment_input_mode", mode.name())
                .build();
        client.analyze(request, new SmallModelCallback<SmallModelResult>() {
            @Override
            public void onSuccess(SmallModelResult value) {
                String rawOutput = value == null ? "" : value.rawText();
                long inferenceMs = value == null ? -1L : value.latencyMs();
                dispatch(callback, IconExperimentRunResult.builder()
                        .setRunId(runId)
                        .setCreatedAtMs(createdAtMs)
                        .setTestsetId(testSet == null ? "" : testSet.testsetId())
                        .setImage(testSet == null ? "" : testSet.image())
                        .setInputMode(mode)
                        .setTargetCount(targetCount(testSet))
                        .setImagePrepareMs(input.imagePrepareMs())
                        .setModelLoadMs(modelLoadMs)
                        .setInferenceMs(inferenceMs)
                        .setTotalMs(System.currentTimeMillis() - startedAtMs)
                        .setPrompt(input.prompt())
                        .setTargets(testSet == null ? null : testSet.targets())
                        .setRawOutput(rawOutput)
                        .setParsedOutput(IconOutputParser.parse(rawOutput))
                        .setManualScores(defaultManualScores(testSet))
                        .build());
            }

            @Override
            public void onError(SmallModelError error) {
                dispatch(callback, failureResult(
                        runId,
                        createdAtMs,
                        testSet,
                        mode,
                        modelLoadMs,
                        input.imagePrepareMs(),
                        System.currentTimeMillis() - startedAtMs,
                        input.prompt(),
                        "",
                        new IconExperimentError(
                                error == null ? SmallModelError.CODE_INFERENCE_FAILED : error.code(),
                                error == null ? "" : error.message()
                        )
                ));
            }
        });
    }

    private static IconExperimentRunResult failureResult(
            String runId,
            long createdAtMs,
            IconExperimentTestSet testSet,
            IconInputMode inputMode,
            long modelLoadMs,
            long imagePrepareMs,
            long totalMs,
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
}
