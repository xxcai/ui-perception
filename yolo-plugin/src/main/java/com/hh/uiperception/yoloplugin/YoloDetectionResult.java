package com.hh.uiperception.yoloplugin;

import java.util.List;

/**
 * 一次 YOLO 检测的完整结果。
 */
public final class YoloDetectionResult {

    public final List<YoloDetection> detections;
    public final int classCount;
    public final long preprocessMs;
    public final long inferenceMs;
    public final long postprocessMs;

    public YoloDetectionResult(List<YoloDetection> detections, int classCount,
                               long preprocessMs, long inferenceMs, long postprocessMs) {
        this.detections = detections;
        this.classCount = classCount;
        this.preprocessMs = preprocessMs;
        this.inferenceMs = inferenceMs;
        this.postprocessMs = postprocessMs;
    }

    public long totalMs() {
        return preprocessMs + inferenceMs + postprocessMs;
    }
}
