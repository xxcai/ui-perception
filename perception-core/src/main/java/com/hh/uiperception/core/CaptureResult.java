package com.hh.uiperception.core;

/**
 * 抓取结果：通道执行一次抓取后的输出。
 *
 * 成功时包含结构化数据（如 XML 字符串）和元信息。
 * 失败时包含错误描述。
 */
public final class CaptureResult {

    private final boolean success;
    private final String channelName;
    private final String baselineId;
    private final String contentType;
    private final String content;
    private final String activityClassName;
    private final long timestampMs;
    private final String errorMessage;

    private CaptureResult(boolean success, String channelName, String baselineId,
                          String contentType, String content, String activityClassName,
                          long timestampMs, String errorMessage) {
        this.success = success;
        this.channelName = channelName;
        this.baselineId = baselineId;
        this.contentType = contentType;
        this.content = content;
        this.activityClassName = activityClassName;
        this.timestampMs = timestampMs;
        this.errorMessage = errorMessage;
    }

    /**
     * 创建成功结果。
     */
    public static CaptureResult success(String channelName, String baselineId,
                                         String contentType, String content,
                                         String activityClassName) {
        return new CaptureResult(true, channelName, baselineId,
                contentType, content, activityClassName,
                System.currentTimeMillis(), null);
    }

    /**
     * 创建失败结果。
     */
    public static CaptureResult error(String channelName, String baselineId,
                                       String errorMessage) {
        return new CaptureResult(false, channelName, baselineId,
                null, null, null,
                System.currentTimeMillis(), errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String channelName() {
        return channelName;
    }

    public String baselineId() {
        return baselineId;
    }

    public String contentType() {
        return contentType;
    }

    public String content() {
        return content;
    }

    public String activityClassName() {
        return activityClassName;
    }

    public long timestampMs() {
        return timestampMs;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
