package com.hh.uiperception.nativeplugin.semantic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Android 屏幕坐标系中的节点矩形。
 */
public final class NativeBounds {

    private static final Pattern BOUNDS_PATTERN =
            Pattern.compile("\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]");

    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    public NativeBounds(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static NativeBounds parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = BOUNDS_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return null;
        }
        return new NativeBounds(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4))
        );
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

    public int centerX() {
        return (left + right) / 2;
    }

    public int centerY() {
        return (top + bottom) / 2;
    }

    public boolean isValid() {
        return right > left && bottom > top;
    }

    public String toSnapshotValue() {
        return left + "," + top + "," + right + "," + bottom;
    }
}
