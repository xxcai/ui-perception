package com.hh.uiperception.smallmodelplugin.experiment;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图标识别实验的一次模型输入。
 */
public final class IconExperimentInput {

    private final IconInputMode inputMode;
    private final Bitmap image;
    private final String prompt;
    private final List<IconTargetMapping> mappings;
    private final long imagePrepareMs;

    public IconExperimentInput(
            IconInputMode inputMode,
            Bitmap image,
            String prompt,
            List<IconTargetMapping> mappings,
            long imagePrepareMs
    ) {
        this.inputMode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        this.image = image;
        this.prompt = prompt == null ? "" : prompt;
        this.mappings = Collections.unmodifiableList(new ArrayList<>(
                mappings == null ? Collections.emptyList() : mappings
        ));
        this.imagePrepareMs = imagePrepareMs;
    }

    public IconInputMode inputMode() {
        return inputMode;
    }

    public Bitmap image() {
        return image;
    }

    public String prompt() {
        return prompt;
    }

    public List<IconTargetMapping> mappings() {
        return mappings;
    }

    public long imagePrepareMs() {
        return imagePrepareMs;
    }
}
