package com.hh.uiperception.nativeplugin.evaluation;

/**
 * 路线二评测中的页面目标标注。
 */
public final class EvaluationTarget {

    private final String id;
    private final String role;
    private final String name;
    private final boolean requiredRef;
    private final String bounds;

    public EvaluationTarget(String id, String role, String name, boolean requiredRef, String bounds) {
        this.id = id;
        this.role = role;
        this.name = name;
        this.requiredRef = requiredRef;
        this.bounds = bounds;
    }

    /** 标识，报告引用。 */
    public String id() {
        return id;
    }

    /** 期望 native semantic role。 */
    public String role() {
        return role;
    }

    /** 期望节点名称，可为空。 */
    public String name() {
        return name;
    }

    /** 是否要求该目标在 snapshot 中具备可执行 ref。 */
    public boolean requiredRef() {
        return requiredRef;
    }

    /** 期望 bounds，第一步仅保留字符串，后续指标再解析。 */
    public String bounds() {
        return bounds;
    }
}
