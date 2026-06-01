package com.hh.uiperception.florence2plugin;

import android.graphics.Bitmap;

/**
 * 单个 icon 的 YOLO 检测 + Florence-2 caption 结果。
 */
public class IconCaptionResult {

    public final int index;
    public final String yoloClass;
    public final float yoloConfidence;
    public final float x1, y1, x2, y2;
    public final String caption;
    public final long florence2Ms;
    public final Bitmap cropBitmap;

    public IconCaptionResult(int index, String yoloClass, float yoloConfidence,
                             float x1, float y1, float x2, float y2,
                             String caption, long florence2Ms, Bitmap cropBitmap) {
        this.index = index;
        this.yoloClass = yoloClass;
        this.yoloConfidence = yoloConfidence;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.caption = caption;
        this.florence2Ms = florence2Ms;
        this.cropBitmap = cropBitmap;
    }
}
