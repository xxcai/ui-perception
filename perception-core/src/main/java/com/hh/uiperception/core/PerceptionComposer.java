package com.hh.uiperception.core;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 感知编排器：按上层请求串联技术工具。
 *
 * 当前实现为最小串行链路：capture -> optional trim。
 * 后续可在此基础上扩展多源融合或任务图。
 */
public final class PerceptionComposer {

    private PerceptionComposer() {
    }

    public static List<PerceptionResult> execute(Activity activity, List<PerceptionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<PerceptionResult> results = new ArrayList<>(requests.size());
        for (PerceptionRequest request : requests) {
            results.add(executeSingle(activity, request));
        }
        return results;
    }

    /**
     * 执行一个多技术方向感知计划。
     */
    public static PerceptionRunResult execute(Activity activity, PerceptionPlan plan) {
        long startedAtMs = System.currentTimeMillis();
        List<PerceptionEntryResult> entries = new ArrayList<>(plan.plugins().size());
        for (PerceptionPlugin plugin : plan.plugins()) {
            entries.add(executePlugin(activity, plan, plugin));
        }
        return new PerceptionRunResult(
                plan.baselineId(),
                plan.runId(),
                startedAtMs,
                System.currentTimeMillis(),
                entries
        );
    }

    private static PerceptionResult executeSingle(Activity activity, PerceptionRequest request) {
        CaptureResult captureResult = executeCapture(activity, request);
        if (!captureResult.isSuccess() || !request.hasTrimStep()) {
            return new PerceptionResult(captureResult, null);
        }

        TrimResult trimResult = executeTrim(request, captureResult);
        return new PerceptionResult(captureResult, trimResult);
    }

    private static PerceptionEntryResult executePlugin(Activity activity, PerceptionPlan plan,
                                                       PerceptionPlugin plugin) {
        PerceptionRequest request = new PerceptionRequest(plan.baselineId(), plugin, plan.trimEnabled());
        PerceptionResult result = executeSingle(activity, request);
        return new PerceptionEntryResult(plugin.name(), result.captureResult(), result.trimResult());
    }

    private static CaptureResult executeCapture(Activity activity, PerceptionRequest request) {
        if (activity == null) {
            return CaptureResult.error(request.pluginName(), request.baselineId(), "Activity 不能为空");
        }

        CaptureResult result = request.plugin().capture(
                activity,
                new CaptureRequest(request.baselineId(), request.pluginName())
        );
        return result != null ? result : CaptureResult.error(request.pluginName(), request.baselineId(),
                "插件不支持抓取: " + request.pluginName());
    }

    private static TrimResult executeTrim(PerceptionRequest request, CaptureResult captureResult) {
        TrimResult result = request.plugin().trim(TrimRequest.fromCaptureResult(captureResult));
        return result != null ? result : TrimResult.error(request.pluginName(), captureResult.channelName(),
                request.baselineId(), "插件不支持裁剪: " + request.pluginName());
    }
}
