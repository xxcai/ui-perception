package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * 单个 target 的自动评判结果。
 */
public final class IconMatchResult {

    private final String id;
    private final String expected;
    private final String actual;
    private final boolean matched;

    public IconMatchResult(String id, String expected, String actual, boolean matched) {
        this.id = id == null ? "" : id.trim();
        this.expected = expected == null ? "" : expected.trim();
        this.actual = actual == null ? "" : actual.trim();
        this.matched = matched;
    }

    public String id() {
        return id;
    }

    public String expected() {
        return expected;
    }

    public String actual() {
        return actual;
    }

    public boolean matched() {
        return matched;
    }
}
