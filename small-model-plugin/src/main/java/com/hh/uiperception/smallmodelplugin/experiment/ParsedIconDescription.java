package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * 小模型按 <id>:<desc> 格式输出后的单条解析结果。
 */
public final class ParsedIconDescription {

    private final String id;
    private final String desc;

    public ParsedIconDescription(String id, String desc) {
        this.id = normalize(id);
        this.desc = normalize(desc);
    }

    public String id() {
        return id;
    }

    public String desc() {
        return desc;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
