package com.hh.uiperception.smallmodelplugin.experiment;

import android.content.Context;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    public static List<IconExperimentRunResult> listResults(Context context) {
        File dir = runsDir(context);
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        List<IconExperimentRunResult> results = new ArrayList<>();
        for (File file : files) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] bytes = inputStream.readAllBytes();
                String json = new String(bytes, StandardCharsets.UTF_8);
                results.add(IconExperimentJson.parseRunResult(json));
            } catch (Exception ignored) {
            }
        }
        return results;
    }

    public static String formatHistorySummary(List<IconExperimentRunResult> results, int maxItems) {
        if (results == null || results.isEmpty()) {
            return "暂无历史数据";
        }
        int count = Math.min(results.size(), maxItems);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            IconExperimentRunResult result = results.get(i);
            List<IconMatchResult> matches = IconResultMatcher.match(
                    result.parsedOutput(), result.targets());
            int matched = IconResultMatcher.matchCount(matches);
            int total = result.targets() == null ? 0 : result.targets().size();
            String time = formatTime(result.createdAtMs());
            builder.append(String.format("%d. %s  %-12s  %d/%d",
                    i + 1, time, abbreviateMode(result.inputMode()),
                    matched, total));
            if (total > 0) {
                builder.append(String.format("  %.0f%%", matched * 100.0 / total));
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    private static String formatTime(long createdAtMs) {
        if (createdAtMs <= 0) {
            return "--:--";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
        return sdf.format(new java.util.Date(createdAtMs));
    }

    private static String abbreviateMode(IconInputMode mode) {
        if (mode == null) {
            return "?";
        }
        switch (mode) {
            case FULL_IMAGE:
                return "FULL";
            case FULL_IMAGE_WITH_BOUNDS:
                return "BOUNDS";
            case FULL_IMAGE_WITH_MARKED_BOUNDS:
                return "MARKED";
            case FULL_IMAGE_WITH_BOUNDS_BATCHED:
                return "BATCHED";
            case CROPPED_MONTAGE:
                return "MONTAGE";
            default:
                return mode.name();
        }
    }
}
