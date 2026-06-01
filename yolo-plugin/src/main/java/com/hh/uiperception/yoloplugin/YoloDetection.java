package com.hh.uiperception.yoloplugin;

/**
 * 单个 YOLO 检测结果，坐标为原图像素坐标系。
 */
public final class YoloDetection {

    public final float x1;
    public final float y1;
    public final float x2;
    public final float y2;
    public final float score;
    public final int classId;
    public final String className;

    public YoloDetection(float x1, float y1, float x2, float y2,
                         float score, int classId, String className) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.score = score;
        this.classId = classId;
        this.className = className;
    }

    public float width() { return x2 - x1; }
    public float height() { return y2 - y1; }
    public float centerX() { return (x1 + x2) / 2f; }
    public float centerY() { return (y1 + y2) / 2f; }
}
