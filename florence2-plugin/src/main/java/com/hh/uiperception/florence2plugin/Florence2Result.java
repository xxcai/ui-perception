package com.hh.uiperception.florence2plugin;

/**
 * Florence-2 推理结果，包含分阶段耗时。
 */
public class Florence2Result {

    public final String caption;
    public final int tokenCount;
    public final long preprocessMs;
    public final long visionEncoderMs;
    public final long encoderMs;
    public final long decoderMs;
    public final long totalMs;

    public Florence2Result(String caption, int tokenCount,
                           long preprocessMs, long visionEncoderMs,
                           long encoderMs, long decoderMs, long totalMs) {
        this.caption = caption;
        this.tokenCount = tokenCount;
        this.preprocessMs = preprocessMs;
        this.visionEncoderMs = visionEncoderMs;
        this.encoderMs = encoderMs;
        this.decoderMs = decoderMs;
        this.totalMs = totalMs;
    }
}
