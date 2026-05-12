package com.hh.uiperception.smallmodelplugin.experiment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * 在截图上直接绘制目标框和 id，避免模型只依赖文本坐标定位。
 */
public final class IconBoundsAnnotator {

    private IconBoundsAnnotator() {
    }

    public static Bitmap annotate(Bitmap source, java.util.List<IconTargetMapping> mappings) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        Bitmap annotated = Bitmap.createBitmap(
                source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(annotated);
        canvas.drawBitmap(source, 0f, 0f, null);
        if (mappings == null) {
            return annotated;
        }

        float strokeWidth = Math.max(4f, source.getWidth() / 240f);
        float labelTextSize = Math.max(22f, source.getWidth() / 46f);
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(Color.rgb(0, 120, 255));
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xDD0078FF);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(labelTextSize);

        for (IconTargetMapping mapping : mappings) {
            IconBounds bounds = mapping.originalBounds();
            if (bounds == null || !bounds.isValid()) {
                continue;
            }
            float left = clamp(bounds.left(), 0, source.getWidth());
            float top = clamp(bounds.top(), 0, source.getHeight());
            float right = clamp(bounds.right(), 0, source.getWidth());
            float bottom = clamp(bounds.bottom(), 0, source.getHeight());
            if (right <= left || bottom <= top) {
                continue;
            }
            canvas.drawRect(left, top, right, bottom, strokePaint);

            float padding = Math.max(5f, source.getWidth() / 260f);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float labelWidth = textPaint.measureText(mapping.label());
            float labelHeight = metrics.descent - metrics.ascent;
            float labelLeft = left;
            float labelTop = Math.max(0f, top - labelHeight - padding * 2f);
            float labelRight = Math.min(source.getWidth(), labelLeft + labelWidth + padding * 2f);
            float labelBottom = labelTop + labelHeight + padding * 2f;
            canvas.drawRect(labelLeft, labelTop, labelRight, labelBottom, bgPaint);
            canvas.drawText(mapping.label(), labelLeft + padding,
                    labelBottom - padding - metrics.descent, textPaint);
        }
        return annotated;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
