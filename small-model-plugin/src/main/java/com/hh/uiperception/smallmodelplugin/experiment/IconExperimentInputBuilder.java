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
        IconExperimentInput input;
        if (mode == IconInputMode.CROPPED_MONTAGE) {
            input = IconMontageBuilder.build(sourceBitmap, testSet);
        } else if (mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS) {
            Bitmap annotatedBitmap = IconBoundsAnnotator.annotate(sourceBitmap, testSet);
            List<IconTargetMapping> mappings = fullImageMappings(testSet);
            input = new IconExperimentInput(
                    mode,
                    annotatedBitmap,
                    IconExperimentPromptBuilder.markedBoundsPrompt(testSet),
                    mappings,
                    -1L
            );
        } else {
            List<IconTargetMapping> mappings = fullImageMappings(testSet);
            input = new IconExperimentInput(
                    mode,
                    sourceBitmap,
                    promptForFullImageMode(sourceBitmap, testSet, mode, mappings),
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
            IconExperimentTestSet testSet,
            IconInputMode mode,
            List<IconTargetMapping> mappings
    ) {
        if (mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED) {
            return IconExperimentPromptBuilder.fullImageWithBoundsPrompt(
                    testSet,
                    sourceBitmap.getWidth(),
                    sourceBitmap.getHeight()
            );
        }
        return IconExperimentPromptBuilder.build(testSet, mode, mappings);
    }

    private static List<IconTargetMapping> fullImageMappings(IconExperimentTestSet testSet) {
        List<IconTargetMapping> mappings = new ArrayList<>();
        if (testSet == null) {
            return mappings;
        }
        for (IconTarget target : testSet.targets()) {
            mappings.add(new IconTargetMapping(
                    target.id(),
                    target.bounds(),
                    target.bounds(),
                    target.id()
            ));
        }
        return mappings;
    }
}
