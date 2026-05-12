package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次图标识别实验运行结果。
 */
public final class IconExperimentRunResult {

    private final String runId;
    private final long createdAtMs;
    private final String testsetId;
    private final String image;
    private final IconInputMode inputMode;
    private final int targetCount;
    private final long imagePrepareMs;
    private final int inputImageWidth;
    private final int inputImageHeight;
    private final int encodedImageWidth;
    private final int encodedImageHeight;
    private final int encodedImageBytes;
    private final long imageEncodeMs;
    private final long modelCallMs;
    private final long modelLoadMs;
    private final long inferenceMs;
    private final long totalMs;
    private final int batchSize;
    private final int batchCount;
    private final String prompt;
    private final List<IconTarget> targets;
    private final String rawOutput;
    private final List<ParsedIconDescription> parsedOutput;
    private final List<IconManualScore> manualScores;
    private final IconExperimentError error;

    private IconExperimentRunResult(Builder builder) {
        this.runId = normalize(builder.runId);
        this.createdAtMs = builder.createdAtMs;
        this.testsetId = normalize(builder.testsetId);
        this.image = normalize(builder.image);
        this.inputMode = builder.inputMode;
        this.targetCount = builder.targetCount;
        this.imagePrepareMs = builder.imagePrepareMs;
        this.inputImageWidth = builder.inputImageWidth;
        this.inputImageHeight = builder.inputImageHeight;
        this.encodedImageWidth = builder.encodedImageWidth;
        this.encodedImageHeight = builder.encodedImageHeight;
        this.encodedImageBytes = builder.encodedImageBytes;
        this.imageEncodeMs = builder.imageEncodeMs;
        this.modelCallMs = builder.modelCallMs;
        this.modelLoadMs = builder.modelLoadMs;
        this.inferenceMs = builder.inferenceMs;
        this.totalMs = builder.totalMs;
        this.batchSize = builder.batchSize;
        this.batchCount = builder.batchCount;
        this.prompt = builder.prompt == null ? "" : builder.prompt;
        this.targets = immutableCopy(builder.targets);
        this.rawOutput = builder.rawOutput == null ? "" : builder.rawOutput;
        this.parsedOutput = immutableCopy(builder.parsedOutput);
        this.manualScores = immutableCopy(builder.manualScores);
        this.error = builder.error;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String runId() {
        return runId;
    }

    public long createdAtMs() {
        return createdAtMs;
    }

    public String testsetId() {
        return testsetId;
    }

    public String image() {
        return image;
    }

    public IconInputMode inputMode() {
        return inputMode;
    }

    public int targetCount() {
        return targetCount;
    }

    public long imagePrepareMs() {
        return imagePrepareMs;
    }

    public int inputImageWidth() {
        return inputImageWidth;
    }

    public int inputImageHeight() {
        return inputImageHeight;
    }

    public int encodedImageWidth() {
        return encodedImageWidth;
    }

    public int encodedImageHeight() {
        return encodedImageHeight;
    }

    public int encodedImageBytes() {
        return encodedImageBytes;
    }

    public long imageEncodeMs() {
        return imageEncodeMs;
    }

    public long modelCallMs() {
        return modelCallMs;
    }

    public long modelLoadMs() {
        return modelLoadMs;
    }

    public long inferenceMs() {
        return inferenceMs;
    }

    public long totalMs() {
        return totalMs;
    }

    public int batchSize() {
        return batchSize;
    }

    public int batchCount() {
        return batchCount;
    }

    public String prompt() {
        return prompt;
    }

    public List<IconTarget> targets() {
        return targets;
    }

    public String rawOutput() {
        return rawOutput;
    }

    public List<ParsedIconDescription> parsedOutput() {
        return parsedOutput;
    }

    public List<IconManualScore> manualScores() {
        return manualScores;
    }

    public IconExperimentError error() {
        return error;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(
                values == null ? Collections.emptyList() : values
        ));
    }

    public static final class Builder {
        private String runId = "";
        private long createdAtMs;
        private String testsetId = "";
        private String image = "";
        private IconInputMode inputMode = IconInputMode.FULL_IMAGE;
        private int targetCount;
        private long imagePrepareMs = -1L;
        private int inputImageWidth;
        private int inputImageHeight;
        private int encodedImageWidth;
        private int encodedImageHeight;
        private int encodedImageBytes;
        private long imageEncodeMs = -1L;
        private long modelCallMs = -1L;
        private long modelLoadMs = -1L;
        private long inferenceMs = -1L;
        private long totalMs = -1L;
        private int batchSize = 0;
        private int batchCount = 1;
        private String prompt = "";
        private List<IconTarget> targets = Collections.emptyList();
        private String rawOutput = "";
        private List<ParsedIconDescription> parsedOutput = Collections.emptyList();
        private List<IconManualScore> manualScores = Collections.emptyList();
        private IconExperimentError error;

        private Builder() {
        }

        public Builder setRunId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder setCreatedAtMs(long createdAtMs) {
            this.createdAtMs = createdAtMs;
            return this;
        }

        public Builder setTestsetId(String testsetId) {
            this.testsetId = testsetId;
            return this;
        }

        public Builder setImage(String image) {
            this.image = image;
            return this;
        }

        public Builder setInputMode(IconInputMode inputMode) {
            this.inputMode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
            return this;
        }

        public Builder setTargetCount(int targetCount) {
            this.targetCount = targetCount;
            return this;
        }

        public Builder setImagePrepareMs(long imagePrepareMs) {
            this.imagePrepareMs = imagePrepareMs;
            return this;
        }

        public Builder setInputImageWidth(int inputImageWidth) {
            this.inputImageWidth = Math.max(0, inputImageWidth);
            return this;
        }

        public Builder setInputImageHeight(int inputImageHeight) {
            this.inputImageHeight = Math.max(0, inputImageHeight);
            return this;
        }

        public Builder setEncodedImageWidth(int encodedImageWidth) {
            this.encodedImageWidth = Math.max(0, encodedImageWidth);
            return this;
        }

        public Builder setEncodedImageHeight(int encodedImageHeight) {
            this.encodedImageHeight = Math.max(0, encodedImageHeight);
            return this;
        }

        public Builder setEncodedImageBytes(int encodedImageBytes) {
            this.encodedImageBytes = Math.max(0, encodedImageBytes);
            return this;
        }

        public Builder setImageEncodeMs(long imageEncodeMs) {
            this.imageEncodeMs = imageEncodeMs;
            return this;
        }

        public Builder setModelCallMs(long modelCallMs) {
            this.modelCallMs = modelCallMs;
            return this;
        }

        public Builder setModelLoadMs(long modelLoadMs) {
            this.modelLoadMs = modelLoadMs;
            return this;
        }

        public Builder setInferenceMs(long inferenceMs) {
            this.inferenceMs = inferenceMs;
            return this;
        }

        public Builder setTotalMs(long totalMs) {
            this.totalMs = totalMs;
            return this;
        }

        public Builder setBatchSize(int batchSize) {
            this.batchSize = Math.max(0, batchSize);
            return this;
        }

        public Builder setBatchCount(int batchCount) {
            this.batchCount = Math.max(1, batchCount);
            return this;
        }

        public Builder setPrompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder setTargets(List<IconTarget> targets) {
            this.targets = targets;
            return this;
        }

        public Builder setRawOutput(String rawOutput) {
            this.rawOutput = rawOutput;
            return this;
        }

        public Builder setParsedOutput(List<ParsedIconDescription> parsedOutput) {
            this.parsedOutput = parsedOutput;
            return this;
        }

        public Builder setManualScores(List<IconManualScore> manualScores) {
            this.manualScores = manualScores;
            return this;
        }

        public Builder setError(IconExperimentError error) {
            this.error = error;
            return this;
        }

        public IconExperimentRunResult build() {
            return new IconExperimentRunResult(this);
        }
    }
}
