package com.hh.uiperception.smallmodelplugin.api;

import android.graphics.Bitmap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次小模型视觉理解请求。
 */
public final class SmallModelRequest {

    private final Bitmap image;
    private final String prompt;
    private final String baselineId;
    private final Map<String, String> options;

    private SmallModelRequest(Builder builder) {
        this.image = builder.image;
        this.prompt = builder.prompt == null ? "" : builder.prompt;
        this.baselineId = builder.baselineId == null ? "" : builder.baselineId;
        this.options = Collections.unmodifiableMap(new LinkedHashMap<>(builder.options));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Bitmap image() {
        return image;
    }

    public String prompt() {
        return prompt;
    }

    public String baselineId() {
        return baselineId;
    }

    public Map<String, String> options() {
        return options;
    }

    public static final class Builder {
        private Bitmap image;
        private String prompt = "";
        private String baselineId = "";
        private final Map<String, String> options = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder setImage(Bitmap image) {
            this.image = image;
            return this;
        }

        public Builder setPrompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder setBaselineId(String baselineId) {
            this.baselineId = baselineId;
            return this;
        }

        public Builder putOption(String key, String value) {
            if (key != null && !key.trim().isEmpty()) {
                options.put(key, value == null ? "" : value);
            }
            return this;
        }

        public SmallModelRequest build() {
            return new SmallModelRequest(this);
        }
    }
}
