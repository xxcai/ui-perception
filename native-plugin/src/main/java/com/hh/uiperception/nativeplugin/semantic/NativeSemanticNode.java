package com.hh.uiperception.nativeplugin.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 面向模型消费的 native 语义节点。
 */
public final class NativeSemanticNode {

    private final NativeSemanticRole role;
    private final String name;
    private final String text;
    private final String contentDescription;
    private final String resourceId;
    private final String className;
    private final NativeBounds bounds;
    private final String ref;
    private final NativeRoleDecision roleDecision;
    private final List<String> states;
    private final List<NativeSemanticNode> children;

    private NativeSemanticNode(Builder builder) {
        if (builder.role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        this.role = builder.role;
        this.name = normalize(builder.name);
        this.text = normalize(builder.text);
        this.contentDescription = normalize(builder.contentDescription);
        this.resourceId = normalize(builder.resourceId);
        this.className = normalize(builder.className);
        this.bounds = builder.bounds;
        this.ref = normalize(builder.ref);
        this.roleDecision = builder.roleDecision;
        this.states = Collections.unmodifiableList(new ArrayList<>(builder.states));
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
    }

    public static Builder builder(NativeSemanticRole role) {
        return new Builder(role);
    }

    public NativeSemanticRole role() {
        return role;
    }

    public String name() {
        return name;
    }

    public String text() {
        return text;
    }

    public String contentDescription() {
        return contentDescription;
    }

    public String resourceId() {
        return resourceId;
    }

    public String className() {
        return className;
    }

    public NativeBounds bounds() {
        return bounds;
    }

    public String ref() {
        return ref;
    }

    public NativeRoleDecision roleDecision() {
        return roleDecision;
    }

    public List<String> states() {
        return states;
    }

    public List<NativeSemanticNode> children() {
        return children;
    }

    public boolean hasRef() {
        return !ref.isEmpty();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static final class Builder {
        private final NativeSemanticRole role;
        private String name;
        private String text;
        private String contentDescription;
        private String resourceId;
        private String className;
        private NativeBounds bounds;
        private String ref;
        private NativeRoleDecision roleDecision;
        private final List<String> states = new ArrayList<>();
        private final List<NativeSemanticNode> children = new ArrayList<>();

        private Builder(NativeSemanticRole role) {
            this.role = role;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder contentDescription(String contentDescription) {
            this.contentDescription = contentDescription;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder bounds(NativeBounds bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public Builder roleDecision(NativeRoleDecision roleDecision) {
            this.roleDecision = roleDecision;
            return this;
        }

        public Builder addState(String state) {
            String normalized = normalize(state);
            if (!normalized.isEmpty()) {
                states.add(normalized);
            }
            return this;
        }

        public Builder addChild(NativeSemanticNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public NativeSemanticNode build() {
            return new NativeSemanticNode(this);
        }
    }
}
