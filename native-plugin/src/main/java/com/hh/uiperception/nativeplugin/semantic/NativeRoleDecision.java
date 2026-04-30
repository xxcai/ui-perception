package com.hh.uiperception.nativeplugin.semantic;

/**
 * role 推导结果。role 是可演进的语义判断，不是原始事实。
 */
public final class NativeRoleDecision {

    private final NativeSemanticRole role;
    private final String source;
    private final double confidence;

    public NativeRoleDecision(NativeSemanticRole role, String source, double confidence) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        this.role = role;
        this.source = source == null ? "" : source;
        this.confidence = confidence;
    }

    public NativeSemanticRole role() {
        return role;
    }

    public String source() {
        return source;
    }

    public double confidence() {
        return confidence;
    }
}
