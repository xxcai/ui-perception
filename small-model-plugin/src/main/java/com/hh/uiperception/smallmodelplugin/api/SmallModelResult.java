package com.hh.uiperception.smallmodelplugin.api;

/**
 * 小模型视觉理解结果。
 */
public final class SmallModelResult {

    private final String rawText;
    private final String normalizedYaml;
    private final long latencyMs;
    private final int inputWidth;
    private final int inputHeight;
    private final int encodedWidth;
    private final int encodedHeight;
    private final int imageBytes;
    private final long imageEncodeMs;
    private final long modelCallMs;

    public SmallModelResult(String rawText, String normalizedYaml, long latencyMs) {
        this(rawText, normalizedYaml, latencyMs, 0, 0, 0, 0, 0, -1L, -1L);
    }

    public SmallModelResult(String rawText, String normalizedYaml, long latencyMs,
                            int inputWidth, int inputHeight,
                            int encodedWidth, int encodedHeight, int imageBytes,
                            long imageEncodeMs, long modelCallMs) {
        this.rawText = rawText == null ? "" : rawText;
        this.normalizedYaml = normalizedYaml == null ? "" : normalizedYaml;
        this.latencyMs = latencyMs;
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.encodedWidth = encodedWidth;
        this.encodedHeight = encodedHeight;
        this.imageBytes = imageBytes;
        this.imageEncodeMs = imageEncodeMs;
        this.modelCallMs = modelCallMs;
    }

    public String rawText() {
        return rawText;
    }

    public String normalizedYaml() {
        return normalizedYaml;
    }

    public long latencyMs() {
        return latencyMs;
    }

    public int inputWidth() {
        return inputWidth;
    }

    public int inputHeight() {
        return inputHeight;
    }

    public int encodedWidth() {
        return encodedWidth;
    }

    public int encodedHeight() {
        return encodedHeight;
    }

    public int imageBytes() {
        return imageBytes;
    }

    public long imageEncodeMs() {
        return imageEncodeMs;
    }

    public long modelCallMs() {
        return modelCallMs;
    }
}
