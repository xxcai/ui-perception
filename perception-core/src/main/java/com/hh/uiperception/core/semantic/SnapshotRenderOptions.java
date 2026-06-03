package com.hh.uiperception.core.semantic;

/**
 * native semantic snapshot 渲染选项。
 */
public final class SnapshotRenderOptions {

    private final boolean boxes;
    private final int maxDepth;

    private SnapshotRenderOptions(boolean boxes, int maxDepth) {
        this.boxes = boxes;
        this.maxDepth = maxDepth;
    }

    public static SnapshotRenderOptions defaults() {
        return new SnapshotRenderOptions(false, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean boxes() {
        return boxes;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public boolean hasDepthLimit() {
        return maxDepth > 0;
    }

    public static final class Builder {
        private boolean boxes;
        private int maxDepth;

        public Builder boxes(boolean boxes) {
            this.boxes = boxes;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = Math.max(0, maxDepth);
            return this;
        }

        public SnapshotRenderOptions build() {
            return new SnapshotRenderOptions(boxes, maxDepth);
        }
    }
}
