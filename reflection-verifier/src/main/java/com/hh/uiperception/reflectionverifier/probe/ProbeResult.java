package com.hh.uiperception.reflectionverifier.probe;

/**
 * 单个反射探测的结果。
 */
public final class ProbeResult {

    /** 探测名称，如 "AdapterView.mOnItemClickListener" */
    public final String probeName;

    /** 反射是否成功找到字段 */
    public final boolean fieldFound;

    /** 字段值是否非空（或列表非空） */
    public final boolean valueDetected;

    /** 实际找到的字段名 */
    public final String fieldName;

    /** 字段声明所在的类 */
    public final String fieldDeclaringClass;

    /** 异常信息（如有） */
    public final String error;

    public ProbeResult(String probeName, boolean fieldFound, boolean valueDetected,
                       String fieldName, String fieldDeclaringClass, String error) {
        this.probeName = probeName;
        this.fieldFound = fieldFound;
        this.valueDetected = valueDetected;
        this.fieldName = fieldName;
        this.fieldDeclaringClass = fieldDeclaringClass;
        this.error = error;
    }

    @Override
    public String toString() {
        if (error != null) {
            return probeName + ": FAIL (" + error + ")";
        }
        return probeName + ": fieldFound=" + fieldFound + ", valueDetected=" + valueDetected
                + " [" + fieldDeclaringClass + "." + fieldName + "]";
    }
}
