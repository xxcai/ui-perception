package com.hh.uiperception.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次感知执行结果：包含同一基准页面上多个技术方向插件的结果。
 */
public final class PerceptionRunResult {

    private final String baselineId;
    private final String runId;
    private final long startedAtMs;
    private final long finishedAtMs;
    private final List<PerceptionEntryResult> entries;

    public PerceptionRunResult(String baselineId, String runId, long startedAtMs,
                               long finishedAtMs, List<PerceptionEntryResult> entries) {
        this.baselineId = baselineId;
        this.runId = runId;
        this.startedAtMs = startedAtMs;
        this.finishedAtMs = finishedAtMs;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public String baselineId() {
        return baselineId;
    }

    public String runId() {
        return runId;
    }

    public long startedAtMs() {
        return startedAtMs;
    }

    public long finishedAtMs() {
        return finishedAtMs;
    }

    public List<PerceptionEntryResult> entries() {
        return entries;
    }
}
