package com.hh.uiperception.smallmodelplugin.api;

import android.content.Context;

import java.io.File;

/**
 * 小模型初始化配置。
 */
public final class SmallModelInitConfig {

    public static final String DEFAULT_MODEL_RELATIVE_PATH =
            "models/gemma-4-e2b-it/gemma-4-E2B-it.litertlm";

    public static final int DEFAULT_MAX_TOKENS = 4096;
    public static final int DEFAULT_TOP_K = 64;
    public static final double DEFAULT_TOP_P = 0.95d;
    public static final double DEFAULT_TEMPERATURE = 1.0d;

    private final String modelPath;
    private final int maxTokens;
    private final int topK;
    private final double topP;
    private final double temperature;
    private final boolean preferGpu;

    private SmallModelInitConfig(Builder builder) {
        this.modelPath = builder.modelPath;
        this.maxTokens = builder.maxTokens;
        this.topK = builder.topK;
        this.topP = builder.topP;
        this.temperature = builder.temperature;
        this.preferGpu = builder.preferGpu;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SmallModelInitConfig defaultFor(Context context) {
        File baseDir = context == null ? null : context.getFilesDir();
        String path = baseDir == null
                ? DEFAULT_MODEL_RELATIVE_PATH
                : new File(baseDir, DEFAULT_MODEL_RELATIVE_PATH).getAbsolutePath();
        return builder().setModelPath(path).build();
    }

    public String modelPath() {
        return modelPath;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public int topK() {
        return topK;
    }

    public double topP() {
        return topP;
    }

    public double temperature() {
        return temperature;
    }

    public boolean preferGpu() {
        return preferGpu;
    }

    public static final class Builder {
        private String modelPath = "";
        private int maxTokens = DEFAULT_MAX_TOKENS;
        private int topK = DEFAULT_TOP_K;
        private double topP = DEFAULT_TOP_P;
        private double temperature = DEFAULT_TEMPERATURE;
        private boolean preferGpu = true;

        private Builder() {
        }

        public Builder setModelPath(String modelPath) {
            this.modelPath = modelPath == null ? "" : modelPath;
            return this;
        }

        public Builder setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder setTopK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder setTopP(double topP) {
            this.topP = topP;
            return this;
        }

        public Builder setTemperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder setPreferGpu(boolean preferGpu) {
            this.preferGpu = preferGpu;
            return this;
        }

        public SmallModelInitConfig build() {
            return new SmallModelInitConfig(this);
        }
    }
}
