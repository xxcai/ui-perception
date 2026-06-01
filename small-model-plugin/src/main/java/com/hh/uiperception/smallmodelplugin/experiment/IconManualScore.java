package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * 人工评分。score 为 null 表示未评分。
 */
public final class IconManualScore {

    private final String id;
    private final Integer score;
    private final String note;

    public IconManualScore(String id, Integer score, String note) {
        this.id = normalize(id);
        this.score = score;
        this.note = normalize(note);
    }

    public String id() {
        return id;
    }

    public Integer score() {
        return score;
    }

    public String note() {
        return note;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
