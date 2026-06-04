package com.hh.uiperception.core.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 面向模型消费的 native 语义节点。
 */
public final class SemanticNode {

    private static final int NO_WEB_ELEMENT = -1;

    private final SemanticRole role;
    private final String name;
    private final String text;
    private final String contentDescription;
    private final String resourceId;
    private final String className;
    private final Bounds bounds;
    private final String ref;
    private final RoleDecision roleDecision;
    private final List<String> states;
    private final List<SemanticNode> children;
    private final int webElementIdx;

    private SemanticNode(Builder builder) {
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
        this.webElementIdx = builder.webElementIdx;
    }

    public static Builder builder(SemanticRole role) {
        return new Builder(role);
    }

    public SemanticRole role() {
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

    public Bounds bounds() {
        return bounds;
    }

    public String ref() {
        return ref;
    }

    public RoleDecision roleDecision() {
        return roleDecision;
    }

    public List<String> states() {
        return states;
    }

    public List<SemanticNode> children() {
        return children;
    }

    public boolean hasRef() {
        return !ref.isEmpty();
    }

    public int webElementIdx() {
        return webElementIdx;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static final class Builder {
        private final SemanticRole role;
        private String name;
        private String text;
        private String contentDescription;
        private String resourceId;
        private String className;
        private Bounds bounds;
        private String ref;
        private RoleDecision roleDecision;
        private final List<String> states = new ArrayList<>();
        private final List<SemanticNode> children = new ArrayList<>();
        private int webElementIdx = NO_WEB_ELEMENT;

        private Builder(SemanticRole role) {
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

        public Builder bounds(Bounds bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder ref(String ref) {
            this.ref = ref;
            return this;
        }

        public Builder roleDecision(RoleDecision roleDecision) {
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

        public Builder addChild(SemanticNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public Builder webElementIdx(int webElementIdx) {
            this.webElementIdx = webElementIdx;
            return this;
        }

        public SemanticNode build() {
            return new SemanticNode(this);
        }
    }
}
