package com.hh.uiperception.smallmodelplugin.api;

/**
 * 小模型视觉理解结果。
 */
public final class SmallModelResult {

    private final String rawText;
    private final String normalizedYaml;
    private final long latencyMs;

    public SmallModelResult(String rawText, String normalizedYaml, long latencyMs) {
        this.rawText = rawText == null ? "" : rawText;
        this.normalizedYaml = normalizedYaml == null ? "" : normalizedYaml;
        this.latencyMs = latencyMs;
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
}
