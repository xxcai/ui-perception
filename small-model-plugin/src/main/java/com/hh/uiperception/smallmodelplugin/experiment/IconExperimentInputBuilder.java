package com.hh.uiperception.smallmodelplugin.experiment;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据输入方式准备图标识别实验的模型输入。
 */
public final class IconExperimentInputBuilder {

    private IconExperimentInputBuilder() {
    }

    public static IconExperimentInput build(
            Bitmap sourceBitmap,
            IconExperimentTestSet testSet,
            IconInputMode inputMode
    ) {
        if (sourceBitmap == null) {
            throw new IllegalArgumentException("sourceBitmap must not be null");
        }
        long startedAtMs = System.currentTimeMillis();
        IconInputMode mode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        if (mode == IconInputMode.CROPPED_MULTI_IMAGE) {
            throw new UnsupportedOperationException("CROPPED_MULTI_IMAGE is not supported yet");
        }
        List<IconTargetMapping> mappings = buildMappings(testSet);
        IconExperimentInput input;
        if (mode == IconInputMode.CROPPED_MONTAGE) {
            input = IconMontageBuilder.build(sourceBitmap, testSet);
        } else if (mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS) {
            Bitmap annotatedBitmap = IconBoundsAnnotator.annotate(sourceBitmap, mappings);
            input = new IconExperimentInput(
                    mode,
                    annotatedBitmap,
                    IconExperimentPromptBuilder.markedBoundsPrompt(mappings),
                    mappings,
                    -1L
            );
        } else {
            input = new IconExperimentInput(
                    mode,
                    sourceBitmap,
                    promptForFullImageMode(sourceBitmap, mode, mappings),
                    mappings,
                    -1L
            );
        }
        long latencyMs = System.currentTimeMillis() - startedAtMs;
        return new IconExperimentInput(
                input.inputMode(),
                input.image(),
                input.prompt(),
                input.mappings(),
                latencyMs
        );
    }

    private static String promptForFullImageMode(
            Bitmap sourceBitmap,
            IconInputMode mode,
            List<IconTargetMapping> mappings
    ) {
        if (mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED) {
            return IconExperimentPromptBuilder.fullImageWithBoundsPrompt(
                    mappings,
                    sourceBitmap.getWidth(),
                    sourceBitmap.getHeight()
            );
        }
        return IconExperimentPromptBuilder.build(mode, mappings);
    }

    public static List<IconTargetMapping> buildMappings(IconExperimentTestSet testSet) {
        List<IconTargetMapping> mappings = new ArrayList<>();
        if (testSet == null) {
            return mappings;
        }
        List<IconTarget> targets = testSet.targets();
        for (int i = 0; i < targets.size(); i++) {
            IconTarget target = targets.get(i);
            mappings.add(new IconTargetMapping(
                    target.id(),
                    target.bounds(),
                    target.bounds(),
                    String.valueOf(i + 1)
            ));
        }
        return mappings;
    }
}
