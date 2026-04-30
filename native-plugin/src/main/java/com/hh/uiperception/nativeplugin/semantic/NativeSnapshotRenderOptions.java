package com.hh.uiperception.nativeplugin.semantic;

/**
 * native semantic snapshot 渲染选项。
 */
public final class NativeSnapshotRenderOptions {

    private final boolean boxes;
    private final int maxDepth;

    private NativeSnapshotRenderOptions(boolean boxes, int maxDepth) {
        this.boxes = boxes;
        this.maxDepth = maxDepth;
    }

    public static NativeSnapshotRenderOptions defaults() {
        return new NativeSnapshotRenderOptions(false, 0);
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

        public NativeSnapshotRenderOptions build() {
            return new NativeSnapshotRenderOptions(boxes, maxDepth);
        }
    }
}
