package com.hh.uiperception.core;

/**
 * 转换结果：转换工具执行一次处理后的输出。
 */
public final class TransformResult {

    private final boolean success;
    private final String toolName;
    private final String sourceToolName;
    private final String baselineId;
    private final String contentType;
    private final String content;
    private final String activityClassName;
    private final long timestampMs;
    private final String errorMessage;

    private TransformResult(boolean success, String toolName, String sourceToolName, String baselineId,
                            String contentType, String content, String activityClassName,
                            long timestampMs, String errorMessage) {
        this.success = success;
        this.toolName = toolName;
        this.sourceToolName = sourceToolName;
        this.baselineId = baselineId;
        this.contentType = contentType;
        this.content = content;
        this.activityClassName = activityClassName;
        this.timestampMs = timestampMs;
        this.errorMessage = errorMessage;
    }

    public static TransformResult success(String toolName, String sourceToolName, String baselineId,
                                          String contentType, String content, String activityClassName) {
        return new TransformResult(true, toolName, sourceToolName, baselineId,
                contentType, content, activityClassName,
                System.currentTimeMillis(), null);
    }

    public static TransformResult error(String toolName, String sourceToolName, String baselineId,
                                        String errorMessage) {
        return new TransformResult(false, toolName, sourceToolName, baselineId,
                null, null, null,
                System.currentTimeMillis(), errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String toolName() {
        return toolName;
    }

    public String sourceToolName() {
        return sourceToolName;
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
