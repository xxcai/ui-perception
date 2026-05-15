package com.hh.uiperception.smallmodelplugin.api;

import android.graphics.Bitmap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次小模型视觉理解请求。
 */
public final class SmallModelRequest {

    public static final String OPTION_IMAGE_MAX_EDGE = "image_max_edge";
    public static final String OPTION_IMAGE_ENCODING = "image_encoding";
    public static final String OPTION_BITMAP_CONFIG = "bitmap_config";
    public static final String OPTION_IMAGE_CROP = "image_crop";
    public static final String OPTION_BITMAP_CONFIG_ARGB_8888 = "argb_8888";
    public static final String OPTION_BITMAP_CONFIG_RGB_565 = "rgb_565";
    public static final String OPTION_IMAGE_ENCODING_PNG = "png";
    public static final String OPTION_IMAGE_ENCODING_JPEG_90 = "jpeg90";
    public static final String OPTION_IMAGE_ENCODING_JPEG_80 = "jpeg80";
    public static final String OPTION_IMAGE_ENCODING_JPEG_75 = "jpeg75";
    public static final String OPTION_IMAGE_ENCODING_WEBP_90 = "webp90";
    public static final String OPTION_IMAGE_ENCODING_WEBP_75 = "webp75";
    public static final String OPTION_IMAGE_ENCODING_WEBP_LOSSLESS = "webp_lossless";
    public static final String OPTION_IMAGE_CROP_FULL = "full";
    public static final String OPTION_IMAGE_CROP_TOP_40 = "top_40";
    public static final String OPTION_IMAGE_CROP_MIDDLE_40 = "middle_40";
    public static final String OPTION_IMAGE_CROP_BOTTOM_40 = "bottom_40";
    public static final String OPTION_IMAGE_CROP_CENTER_60 = "center_60";

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
