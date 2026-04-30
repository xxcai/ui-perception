package com.hh.uiperception.core;

/**
 * 单个技术方向插件在一次感知执行中的结果。
 */
public final class PerceptionEntryResult {

    private final String pluginName;
    private final CaptureResult captureResult;
    private final TransformResult transformResult;

    public PerceptionEntryResult(String pluginName, CaptureResult captureResult, TransformResult transformResult) {
        this.pluginName = pluginName;
        this.captureResult = captureResult;
        this.transformResult = transformResult;
    }

    public String pluginName() {
        return pluginName;
    }

    public CaptureResult captureResult() {
        return captureResult;
    }

    public TransformResult transformResult() {
        return transformResult;
    }

    public boolean captureSucceeded() {
        return captureResult != null && captureResult.isSuccess();
    }

    public boolean transformSucceeded() {
        return transformResult != null && transformResult.isSuccess();
    }
}
