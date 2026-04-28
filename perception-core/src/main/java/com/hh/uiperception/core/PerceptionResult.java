package com.hh.uiperception.core;

/**
 * 感知编排结果：包含一次抓取结果，以及可选的裁剪结果。
 */
public final class PerceptionResult {

    private final CaptureResult captureResult;
    private final TrimResult trimResult;

    public PerceptionResult(CaptureResult captureResult, TrimResult trimResult) {
        this.captureResult = captureResult;
        this.trimResult = trimResult;
    }

    public CaptureResult captureResult() {
        return captureResult;
    }

    public TrimResult trimResult() {
        return trimResult;
    }

    public boolean hasTrimResult() {
        return trimResult != null;
    }
}
