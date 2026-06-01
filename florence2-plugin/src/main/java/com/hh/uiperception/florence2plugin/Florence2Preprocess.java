package com.hh.uiperception.florence2plugin;

import android.graphics.Bitmap;

/**
 * Florence-2 图像预处理：resize 到 768x768，NCHW float32，ImageNet 标准化。
 */
public final class Florence2Preprocess {

    public static final int INPUT_SIZE = 768;

    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD  = {0.229f, 0.224f, 0.225f};

    private Florence2Preprocess() {}

    /**
     * Bitmap → float[1][3][768][768] NCHW，ImageNet 标准化。
     */
    public static float[] preprocess(Bitmap bitmap) {
        Bitmap resized = bitmap;
        if (bitmap.getWidth() != INPUT_SIZE || bitmap.getHeight() != INPUT_SIZE) {
            resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        }

        int size = INPUT_SIZE;
        int[] pixels = new int[size * size];
        resized.getPixels(pixels, 0, size, 0, 0, size, size);

        int planeSize = size * size;
        float[] output = new float[3 * planeSize];

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            output[i]                = (((pixel >> 16) & 0xFF) / 255f - MEAN[0]) / STD[0]; // R
            output[planeSize + i]    = (((pixel >> 8) & 0xFF) / 255f - MEAN[1]) / STD[1]; // G
            output[2 * planeSize + i]= ((pixel & 0xFF) / 255f - MEAN[2]) / STD[2];        // B
        }

        if (resized != bitmap && !resized.isRecycled()) {
            resized.recycle();
        }
        return output;
    }
}
