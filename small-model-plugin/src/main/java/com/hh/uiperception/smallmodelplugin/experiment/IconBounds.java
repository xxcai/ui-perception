package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * 图标目标在原始截图中的像素坐标。
 */
public final class IconBounds {

    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    public IconBounds(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int right() {
        return right;
    }

    public int bottom() {
        return bottom;
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public boolean isValid() {
        return right > left && bottom > top;
    }
}
