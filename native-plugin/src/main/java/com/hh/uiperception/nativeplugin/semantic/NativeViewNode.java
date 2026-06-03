package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从 native XML 解析出的原始 View 节点属性。
 */
public final class NativeViewNode {

    private final String className;
    private final String text;
    private final String contentDescription;
    private final String resourceId;
    private final Bounds bounds;
    private final boolean clickable;
    private final boolean hasOnClickListener;
    private final boolean hasItemClickListener;
    private final boolean hasItemTouchListener;
    private final boolean longClickable;
    private final boolean enabled;
    private final boolean checked;
    private final boolean selected;
    private final boolean focused;
    private final boolean scrollable;
    private final boolean editable;
    private final boolean password;
    private final List<NativeViewNode> children;

    private NativeViewNode(Builder builder) {
        this.className = normalize(builder.className);
        this.text = normalize(builder.text);
        this.contentDescription = normalize(builder.contentDescription);
        this.resourceId = normalize(builder.resourceId);
        this.bounds = builder.bounds;
        this.clickable = builder.clickable;
        this.hasOnClickListener = builder.hasOnClickListener;
        this.hasItemClickListener = builder.hasItemClickListener;
        this.hasItemTouchListener = builder.hasItemTouchListener;
        this.longClickable = builder.longClickable;
        this.enabled = builder.enabled;
        this.checked = builder.checked;
        this.selected = builder.selected;
        this.focused = builder.focused;
        this.scrollable = builder.scrollable;
        this.editable = builder.editable;
        this.password = builder.password;
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String className() {
        return className;
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

    public Bounds bounds() {
        return bounds;
    }

    public boolean clickable() {
        return clickable;
    }

    public boolean hasOnClickListener() {
        return hasOnClickListener;
    }

    public boolean hasItemClickListener() {
        return hasItemClickListener;
    }

    public boolean hasItemTouchListener() {
        return hasItemTouchListener;
    }

    public boolean longClickable() {
        return longClickable;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean checked() {
        return checked;
    }

    public boolean selected() {
        return selected;
    }

    public boolean focused() {
        return focused;
    }

    public boolean scrollable() {
        return scrollable;
    }

    public boolean editable() {
        return editable;
    }

    public boolean password() {
        return password;
    }

    public List<NativeViewNode> children() {
        return children;
    }

    public boolean hasText() {
        return !text.isEmpty();
    }

    public boolean hasContentDescription() {
        return !contentDescription.isEmpty();
    }

    public boolean hasResourceId() {
        return !resourceId.isEmpty();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    public static final class Builder {
        private String className;
        private String text;
        private String contentDescription;
        private String resourceId;
        private Bounds bounds;
        private boolean clickable;
        private boolean hasOnClickListener;
        private boolean hasItemClickListener;
        private boolean hasItemTouchListener;
        private boolean longClickable;
        private boolean enabled = true;
        private boolean checked;
        private boolean selected;
        private boolean focused;
        private boolean scrollable;
        private boolean editable;
        private boolean password;
        private final List<NativeViewNode> children = new ArrayList<>();

        public Builder className(String className) {
            this.className = className;
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

        public Builder bounds(Bounds bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder clickable(boolean clickable) {
            this.clickable = clickable;
            return this;
        }

        public Builder hasOnClickListener(boolean hasOnClickListener) {
            this.hasOnClickListener = hasOnClickListener;
            return this;
        }

        public Builder hasItemClickListener(boolean hasItemClickListener) {
            this.hasItemClickListener = hasItemClickListener;
            return this;
        }

        public Builder hasItemTouchListener(boolean hasItemTouchListener) {
            this.hasItemTouchListener = hasItemTouchListener;
            return this;
        }

        public Builder longClickable(boolean longClickable) {
            this.longClickable = longClickable;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder checked(boolean checked) {
            this.checked = checked;
            return this;
        }

        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public Builder focused(boolean focused) {
            this.focused = focused;
            return this;
        }

        public Builder scrollable(boolean scrollable) {
            this.scrollable = scrollable;
            return this;
        }

        public Builder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public Builder password(boolean password) {
            this.password = password;
            return this;
        }

        public Builder addChild(NativeViewNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public NativeViewNode build() {
            return new NativeViewNode(this);
        }
    }
}
