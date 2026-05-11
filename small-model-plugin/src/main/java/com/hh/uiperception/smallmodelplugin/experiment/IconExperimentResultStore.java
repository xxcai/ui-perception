package com.hh.uiperception.smallmodelplugin.experiment;

import android.content.Context;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 实验结果文件存储。
 */
public final class IconExperimentResultStore {

    public static final String RELATIVE_RUNS_DIR = "small-model-experiments/runs";

    private IconExperimentResultStore() {
    }

    public static File runsDir(Context context) {
        File baseDir = context == null ? null : context.getExternalFilesDir(null);
        if (baseDir == null && context != null) {
            baseDir = context.getFilesDir();
        }
        return baseDir == null ? new File(RELATIVE_RUNS_DIR) : new File(baseDir, RELATIVE_RUNS_DIR);
    }

    public static File save(Context context, IconExperimentRunResult result)
            throws IOException, JSONException {
        return save(runsDir(context), result);
    }

    static File save(File runsDir, IconExperimentRunResult result)
            throws IOException, JSONException {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        if (!runsDir.exists() && !runsDir.mkdirs()) {
            throw new IOException("failed to create runs dir: " + runsDir.getAbsolutePath());
        }
        File outputFile = new File(runsDir, safeFileName(result.runId()) + ".json");
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(IconExperimentJson.toJson(result).getBytes(StandardCharsets.UTF_8));
        }
        return outputFile;
    }

    private static String safeFileName(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return "icon_experiment_run";
        }
        return text.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
