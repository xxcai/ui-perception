package com.hh.uiperception.smallmodelplugin.api;

/**
 * 小模型能力层错误。
 */
public final class SmallModelError {

    public static final String CODE_MODEL_FILE_MISSING = "MODEL_FILE_MISSING";
    public static final String CODE_UNSUPPORTED_ANDROID_VERSION = "UNSUPPORTED_ANDROID_VERSION";
    public static final String CODE_INITIALIZATION_FAILED = "INITIALIZATION_FAILED";
    public static final String CODE_NOT_INITIALIZED = "NOT_INITIALIZED";
    public static final String CODE_INFERENCE_IN_PROGRESS = "INFERENCE_IN_PROGRESS";
    public static final String CODE_INFERENCE_FAILED = "INFERENCE_FAILED";
    public static final String CODE_INVALID_REQUEST = "INVALID_REQUEST";

    private final String code;
    private final String message;
    private final Throwable cause;

    public SmallModelError(String code, String message) {
        this(code, message, null);
    }

    public SmallModelError(String code, String message, Throwable cause) {
        this.code = code == null ? "" : code;
        this.message = message == null ? "" : message;
        this.cause = cause;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public Throwable cause() {
        return cause;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
