package com.hh.uiperception.core;

/**
 * 感知编排请求：描述一次上层 pipeline 要执行的抓取及可选转换步骤。
 */
public final class PerceptionRequest {

    private final String baselineId;
    private final PerceptionPlugin plugin;
    private final boolean transformEnabled;

    public PerceptionRequest(String baselineId, PerceptionPlugin plugin, boolean transformEnabled) {
        this.baselineId = requireText(baselineId, "baselineId");
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
        this.transformEnabled = transformEnabled;
    }

    public String baselineId() {
        return baselineId;
    }

    public PerceptionPlugin plugin() {
        return plugin;
    }

    public String pluginName() {
        return plugin.name();
    }

    public boolean hasTransformStep() {
        return transformEnabled;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
