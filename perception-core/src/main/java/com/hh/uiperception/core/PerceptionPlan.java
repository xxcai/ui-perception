package com.hh.uiperception.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 感知执行计划：描述同一基准页面上要执行的多个技术方向插件。
 */
public final class PerceptionPlan {

    private final String baselineId;
    private final List<PerceptionPlugin> plugins;
    private final boolean trimEnabled;
    private final String runId;

    public PerceptionPlan(String baselineId, List<PerceptionPlugin> plugins, boolean trimEnabled) {
        this(baselineId, plugins, trimEnabled, String.valueOf(System.currentTimeMillis()));
    }

    public PerceptionPlan(String baselineId, List<PerceptionPlugin> plugins,
                          boolean trimEnabled, String runId) {
        this.baselineId = requireText(baselineId, "baselineId");
        if (plugins == null || plugins.isEmpty()) {
            throw new IllegalArgumentException("plugins must not be empty");
        }
        this.plugins = Collections.unmodifiableList(new ArrayList<>(plugins));
        this.trimEnabled = trimEnabled;
        this.runId = requireText(runId, "runId");
    }

    public String baselineId() {
        return baselineId;
    }

    public List<PerceptionPlugin> plugins() {
        return plugins;
    }

    public boolean trimEnabled() {
        return trimEnabled;
    }

    public String runId() {
        return runId;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
