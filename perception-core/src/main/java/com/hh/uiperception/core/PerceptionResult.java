package com.hh.uiperception.core;

/**
 * 感知编排结果：包含一次抓取结果，以及可选的转换结果。
 */
public final class PerceptionResult {

    private final CaptureResult captureResult;
    private final TransformResult transformResult;

    public PerceptionResult(CaptureResult captureResult, TransformResult transformResult) {
        this.captureResult = captureResult;
        this.transformResult = transformResult;
    }

    public CaptureResult captureResult() {
        return captureResult;
    }

    public TransformResult transformResult() {
        return transformResult;
    }

    public boolean hasTransformResult() {
        return transformResult != null;
    }
}
