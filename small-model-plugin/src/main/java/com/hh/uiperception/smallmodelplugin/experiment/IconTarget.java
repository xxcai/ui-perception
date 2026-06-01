package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个待识别图标目标。
 */
public final class IconTarget {

    private final String id;
    private final IconBounds bounds;
    private final String expected;
    private final List<String> acceptable;

    public IconTarget(String id, IconBounds bounds, String expected, List<String> acceptable) {
        this.id = normalize(id);
        this.bounds = bounds;
        this.expected = normalize(expected);
        this.acceptable = Collections.unmodifiableList(new ArrayList<>(
                acceptable == null ? Collections.emptyList() : acceptable
        ));
    }

    public String id() {
        return id;
    }

    public IconBounds bounds() {
        return bounds;
    }

    public String expected() {
        return expected;
    }

    public List<String> acceptable() {
        return acceptable;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
