package com.hh.uiperception.sdk.internal;

public class CaptureResponse {

    private final boolean success;
    private final String activity;
    private final String yaml;
    private final long timestamp;
    private final String error;

    private CaptureResponse(boolean success, String activity, String yaml, long timestamp, String error) {
        this.success = success;
        this.activity = activity;
        this.yaml = yaml;
        this.timestamp = timestamp;
        this.error = error;
    }

    public static CaptureResponse success(String activity, String yaml) {
        return new CaptureResponse(true, activity, yaml, System.currentTimeMillis(), null);
    }

    public static CaptureResponse error(String message) {
        return new CaptureResponse(false, null, null, 0, message);
    }

    public boolean isSuccess() { return success; }
    public String getActivity() { return activity; }
    public String getYaml() { return yaml; }
    public long getTimestamp() { return timestamp; }
    public String getError() { return error; }

    public String toJson() {
        if (success) {
            return "{\"status\":\"success\",\"result\":{\"activity\":\"" + escape(activity)
                    + "\",\"yaml\":\"" + escape(yaml)
                    + "\",\"timestamp\":" + timestamp + "}}";
        } else {
            return "{\"status\":\"error\",\"error\":\"" + escape(error) + "\"}";
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
