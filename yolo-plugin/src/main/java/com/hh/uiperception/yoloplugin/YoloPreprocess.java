package com.hh.uiperception.yoloplugin;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/**
 * YOLO 预处理：letterbox 缩放 + NCHW 转换。
 * 移植自 X-OmniClaw 的 UiYoloPreprocess.kt。
 */
public final class YoloPreprocess {

    private static final int PAD_COLOR = 114;

    private YoloPreprocess() {}

    /** letterbox 缩放参数，用于后处理时将坐标逆变换回原图。 */
    public static class LetterboxParams {
        public final float ratio;
        public final float padX;
        public final float padY;
        public final int srcWidth;
        public final int srcHeight;
        public final int dstSize;

        public LetterboxParams(float ratio, float padX, float padY,
                               int srcWidth, int srcHeight, int dstSize) {
            this.ratio = ratio;
            this.padX = padX;
            this.padY = padY;
            this.srcWidth = srcWidth;
            this.srcHeight = srcHeight;
            this.dstSize = dstSize;
        }
    }

    /** letterbox + 参数的返回值。 */
    public static class LetterboxResult {
        public final Bitmap bitmap;
        public final LetterboxParams params;

        public LetterboxResult(Bitmap bitmap, LetterboxParams params) {
            this.bitmap = bitmap;
            this.params = params;
        }
    }

    /**
     * 将原图缩放到固定正方形输入，保留宽高比并记录逆变换参数。
     */
    public static LetterboxResult letterbox(Bitmap bitmap, int dstSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = Math.min(dstSize / (float) width, dstSize / (float) height);
        int newWidth = Math.max(1, Math.round(width * ratio));
        int newHeight = Math.max(1, Math.round(height * ratio));
        float padX = (dstSize - newWidth) / 2f;
        float padY = (dstSize - newHeight) / 2f;

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        Bitmap output = Bitmap.createBitmap(dstSize, dstSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(Color.rgb(PAD_COLOR, PAD_COLOR, PAD_COLOR));
        canvas.drawBitmap(scaled, padX, padY, null);
        if (scaled != bitmap && !scaled.isRecycled()) {
            scaled.recycle();
        }

        LetterboxParams params = new LetterboxParams(ratio, padX, padY,
                width, height, dstSize);
        return new LetterboxResult(output, params);
    }

    /**
     * Bitmap → float NCHW，通道顺序 R/G/B，值域 [0,1]。
     */
    public static float[] bitmapToNchw01(Bitmap bitmap, int size) {
        int[] pixels = new int[size * size];
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size);
        float[] output = new float[3 * size * size];
        int planeSize = size * size;
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            output[i] = ((pixel >> 16) & 0xFF) / 255f;                  // R
            output[planeSize + i] = ((pixel >> 8) & 0xFF) / 255f;      // G
            output[2 * planeSize + i] = (pixel & 0xFF) / 255f;         // B
        }
        return output;
    }
}
