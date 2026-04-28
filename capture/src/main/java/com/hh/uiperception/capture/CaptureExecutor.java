package com.hh.uiperception.capture;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 抓取执行器：根据请求列表分发到对应通道执行抓取。
 *
 * 单通道传一个元素的列表，多通道传多个。
 */
public final class CaptureExecutor {

    private CaptureExecutor() {
    }

    /**
     * 在给定 Activity 上执行一组抓取请求。
     *
     * @param activity 前台 Activity
     * @param requests 抓取请求列表
     * @return 与请求一一对应的抓取结果列表
     */
    public static List<CaptureResult> execute(Activity activity, List<CaptureRequest> requests) {
        if (activity == null) {
            return allError(requests, "Activity 不能为空");
        }
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<CaptureResult> results = new ArrayList<>(requests.size());
        for (CaptureRequest request : requests) {
            results.add(executeSingle(activity, request));
        }
        return results;
    }

    private static CaptureResult executeSingle(Activity activity, CaptureRequest request) {
        CaptureChannel channel = CaptureChannelRegistry.findByName(request.channelName());
        if (channel == null) {
            return CaptureResult.error(request.channelName(), request.baselineId(),
                    "未注册的抓取通道: " + request.channelName());
        }
        return channel.capture(activity, request);
    }

    private static List<CaptureResult> allError(List<CaptureRequest> requests, String message) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<CaptureResult> results = new ArrayList<>(requests.size());
        for (CaptureRequest request : requests) {
            results.add(CaptureResult.error(request.channelName(), request.baselineId(), message));
        }
        return results;
    }
}
