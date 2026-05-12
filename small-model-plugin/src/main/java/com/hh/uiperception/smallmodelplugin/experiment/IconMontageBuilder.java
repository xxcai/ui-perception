package com.hh.uiperception.smallmodelplugin.experiment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * 生成图标裁剪拼接图。
 */
public final class IconMontageBuilder {

    private static final int MIN_CROP_PADDING_PX = 12;
    private static final float CROP_PADDING_RATIO = 0.2f;

    private IconMontageBuilder() {
    }

    public static IconExperimentInput build(Bitmap sourceBitmap, IconExperimentTestSet testSet) {
        if (sourceBitmap == null) {
            throw new IllegalArgumentException("sourceBitmap must not be null");
        }
        IconMontageLayout layout = IconMontageLayoutCalculator.calculate(
                testSet == null ? null : testSet.targets()
        );
        Bitmap montage = Bitmap.createBitmap(layout.width(), layout.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(montage);
        canvas.drawColor(Color.WHITE);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.rgb(32, 33, 36));
        textPaint.setTextSize(20f);

        Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setColor(Color.rgb(218, 220, 224));
        cellPaint.setStyle(Paint.Style.STROKE);
        cellPaint.setStrokeWidth(1f);

        for (IconTargetMapping mapping : layout.mappings()) {
            drawCell(sourceBitmap, canvas, textPaint, cellPaint, layout, mapping);
        }
        return new IconExperimentInput(
                IconInputMode.CROPPED_MONTAGE,
                montage,
                IconExperimentPromptBuilder.montagePrompt(layout.mappings()),
                layout.mappings(),
                -1L
        );
    }

    private static void drawCell(
            Bitmap sourceBitmap,
            Canvas canvas,
            Paint textPaint,
            Paint cellPaint,
            IconMontageLayout layout,
            IconTargetMapping mapping
    ) {
        IconBounds inputBounds = mapping.inputBounds();
        int index = layout.mappings().indexOf(mapping);
        int row = index / layout.columns();
        int column = index % layout.columns();
        int cellLeft = column * layout.cellWidth();
        int cellTop = row * layout.cellHeight();
        canvas.drawRect(
                cellLeft,
                cellTop,
                cellLeft + layout.cellWidth(),
                cellTop + layout.cellHeight(),
                cellPaint
        );
        canvas.drawText(mapping.label(), cellLeft + 12f, cellTop + 26f, textPaint);

        IconBounds sourceBounds = expandAndClampBounds(mapping.originalBounds(), sourceBitmap);
        if (sourceBounds == null) {
            return;
        }
        Rect source = new Rect(
                sourceBounds.left(),
                sourceBounds.top(),
                sourceBounds.right(),
                sourceBounds.bottom()
        );
        Rect destination = new Rect(
                inputBounds.left(),
                inputBounds.top(),
                inputBounds.right(),
                inputBounds.bottom()
        );
        canvas.drawBitmap(sourceBitmap, source, destination, null);
    }

    private static IconBounds expandAndClampBounds(IconBounds bounds, Bitmap bitmap) {
        if (bounds == null || bitmap == null || !bounds.isValid()) {
            return null;
        }
        int padding = Math.max(
                MIN_CROP_PADDING_PX,
                Math.round(Math.max(bounds.width(), bounds.height()) * CROP_PADDING_RATIO)
        );
        int left = Math.max(0, Math.min(bounds.left() - padding, bitmap.getWidth() - 1));
        int top = Math.max(0, Math.min(bounds.top() - padding, bitmap.getHeight() - 1));
        int right = Math.max(left + 1, Math.min(bounds.right() + padding, bitmap.getWidth()));
        int bottom = Math.max(top + 1, Math.min(bounds.bottom() + padding, bitmap.getHeight()));
        return new IconBounds(left, top, right, bottom);
    }
}
