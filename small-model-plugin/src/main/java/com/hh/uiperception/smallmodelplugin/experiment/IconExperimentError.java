package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * 实验失败信息。失败 run 也需要被记录。
 */
public final class IconExperimentError {

    private final String code;
    private final String message;

    public IconExperimentError(String code, String message) {
        this.code = normalize(code);
        this.message = normalize(message);
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
