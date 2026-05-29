package com.hh.uiperception.reflectionverifier.probe;

import java.util.List;

/**
 * 一次完整探测的报告，包含对某个 View 的所有探测结果。
 */
public final class ProbeReport {

    /** 被探测的 View 类名 */
    public final String viewClass;

    /** 场景描述，如 "ListView with setOnItemClickListener" */
    public final String scenario;

    /** 所有探测结果 */
    public final List<ProbeResult> results;

    /** 所有探测是否都成功找到字段 */
    public final boolean allFieldsFound;

    /** 是否与期望一致 */
    public final boolean passed;

    public ProbeReport(String viewClass, String scenario, List<ProbeResult> results,
                       boolean allFieldsFound, boolean passed) {
        this.viewClass = viewClass;
        this.scenario = scenario;
        this.results = results;
        this.allFieldsFound = allFieldsFound;
        this.passed = passed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(scenario).append(" [").append(viewClass).append("] ===\n");
        for (ProbeResult r : results) {
            sb.append("  ").append(r.toString()).append("\n");
        }
        sb.append("  RESULT: ").append(passed ? "PASS" : "FAIL").append("\n");
        return sb.toString();
    }
}
