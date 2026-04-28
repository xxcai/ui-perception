package com.hh.uiperception.core;

/**
 * 抓取请求：描述对哪个页面执行哪个通道的抓取。
 */
public final class CaptureRequest {

    private final String baselineId;
    private final String channelName;

    public CaptureRequest(String baselineId, String channelName) {
        this.baselineId = requireText(baselineId, "baselineId");
        this.channelName = requireText(channelName, "channelName");
    }

    public String baselineId() {
        return baselineId;
    }

    public String channelName() {
        return channelName;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
