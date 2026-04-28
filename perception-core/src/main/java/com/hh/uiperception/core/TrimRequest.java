package com.hh.uiperception.core;

/**
 * 裁剪请求：由 composer 基于抓取结果构造，描述待裁剪的原始内容。
 */
public final class TrimRequest {

    private final String baselineId;
    private final String sourceToolName;
    private final String sourceContentType;
    private final String sourceContent;
    private final String activityClassName;

    public TrimRequest(String baselineId, String sourceToolName, String sourceContentType,
                       String sourceContent, String activityClassName) {
        this.baselineId = requireText(baselineId, "baselineId");
        this.sourceToolName = requireText(sourceToolName, "sourceToolName");
        this.sourceContentType = sourceContentType == null ? "" : sourceContentType;
        this.sourceContent = sourceContent == null ? "" : sourceContent;
        this.activityClassName = activityClassName == null ? "" : activityClassName;
    }

    public static TrimRequest fromCaptureResult(CaptureResult result) {
        return new TrimRequest(
                result.baselineId(),
                result.channelName(),
                result.contentType(),
                result.content(),
                result.activityClassName()
        );
    }

    public String baselineId() {
        return baselineId;
    }

    public String sourceToolName() {
        return sourceToolName;
    }

    public String sourceContentType() {
        return sourceContentType;
    }

    public String sourceContent() {
        return sourceContent;
    }

    public String activityClassName() {
        return activityClassName;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
