package com.hh.uiperception.yoloplugin;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.List;

/**
 * 在 Bitmap 上绘制 YOLO 检测框和标签。
 */
public final class YoloAnnotator {

    private static final int[] PALETTE = {
            0xFFE53935, 0xFF43A047, 0xFF1E88E5, 0xFFFB8C00, 0xFF8E24AA,
            0xFF00ACC1, 0xFFFFB300, 0xFF5E35B1, 0xFF00897B, 0xFFF4511E,
            0xFF3949AB, 0xFFD81B60, 0xFF00C853, 0xFF6D4C41, 0xFF546E7A,
            0xFF9C27B0, 0xFF2196F3, 0xFF4CAF50, 0xFFFF9800, 0xFF795548,
            0xFF607D8B
    };

    private YoloAnnotator() {}

    /**
     * 在原图副本上绘制检测框，返回新的 Bitmap。
     */
    public static Bitmap annotate(Bitmap source, List<YoloDetection> detections) {
        Bitmap copy = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(copy);

        float strokeWidth = Math.max(2f, Math.min(source.getWidth(), source.getHeight()) / 300f);
        float textSize = Math.max(12f, Math.min(source.getWidth(), source.getHeight()) / 50f);

        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(strokeWidth);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.WHITE);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        for (YoloDetection det : detections) {
            int color = PALETTE[det.classId % PALETTE.length];
            boxPaint.setColor(color);
            bgPaint.setColor(color);

            RectF rect = new RectF(det.x1, det.y1, det.x2, det.y2);
            canvas.drawRect(rect, boxPaint);

            String label = String.format("%s %.2f", det.className, det.score);
            float textWidth = textPaint.measureText(label);
            float textHeight = textSize;
            float labelLeft = det.x1;
            float labelTop = det.y1 - textHeight - strokeWidth * 2;

            if (labelTop < 0) labelTop = det.y1 + strokeWidth;

            canvas.drawRect(labelLeft - 2, labelTop - 2,
                    labelLeft + textWidth + 4, labelTop + textHeight + 2, bgPaint);
            canvas.drawText(label, labelLeft, labelTop + textHeight - 2, textPaint);
        }

        return copy;
    }
}
