package com.hh.uiperception.core;

/**
 * 单个技术方向插件在一次感知执行中的结果。
 */
public final class PerceptionEntryResult {

    private final String pluginName;
    private final CaptureResult captureResult;
    private final TrimResult trimResult;

    public PerceptionEntryResult(String pluginName, CaptureResult captureResult, TrimResult trimResult) {
        this.pluginName = pluginName;
        this.captureResult = captureResult;
        this.trimResult = trimResult;
    }

    public String pluginName() {
        return pluginName;
    }

    public CaptureResult captureResult() {
        return captureResult;
    }

    public TrimResult trimResult() {
        return trimResult;
    }

    public boolean captureSucceeded() {
        return captureResult != null && captureResult.isSuccess();
    }

    public boolean trimSucceeded() {
        return trimResult != null && trimResult.isSuccess();
    }
}
