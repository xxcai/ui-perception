package com.hh.uiperception.sdk.internal;

public class OperationResponse {

    private final boolean success;
    private final String ref;
    private final String error;

    private OperationResponse(boolean success, String ref, String error) {
        this.success = success;
        this.ref = ref;
        this.error = error;
    }

    public static OperationResponse success(String ref) {
        return new OperationResponse(true, ref, null);
    }

    public static OperationResponse error(String message) {
        return new OperationResponse(false, null, message);
    }

    public boolean isSuccess() { return success; }

    public String toJson() {
        if (success) {
            return "{\"status\":\"success\",\"result\":{\"ref\":\"" + escape(ref) + "\"}}";
        } else {
            return "{\"status\":\"error\",\"error\":\"" + escape(error) + "\"}";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
