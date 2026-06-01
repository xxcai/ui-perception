package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 计算裁剪拼接图布局，不依赖 Android 图形 API，便于单测。
 */
public final class IconMontageLayoutCalculator {

    public static final int DEFAULT_COLUMNS = 3;
    public static final int DEFAULT_CELL_WIDTH = 220;
    public static final int DEFAULT_CELL_HEIGHT = 190;
    public static final int DEFAULT_LABEL_HEIGHT = 38;
    public static final int DEFAULT_PADDING = 16;

    private IconMontageLayoutCalculator() {
    }

    public static IconMontageLayout calculate(List<IconTarget> targets) {
        return calculate(
                targets,
                DEFAULT_COLUMNS,
                DEFAULT_CELL_WIDTH,
                DEFAULT_CELL_HEIGHT,
                DEFAULT_LABEL_HEIGHT,
                DEFAULT_PADDING
        );
    }

    static IconMontageLayout calculate(
            List<IconTarget> targets,
            int columns,
            int cellWidth,
            int cellHeight,
            int labelHeight,
            int padding
    ) {
        List<IconTarget> safeTargets = targets == null ? Collections.emptyList() : targets;
        int safeColumns = Math.max(1, columns);
        int rows = safeTargets.isEmpty()
                ? 0
                : (int) Math.ceil(safeTargets.size() / (double) safeColumns);
        int width = safeColumns * cellWidth;
        int height = Math.max(1, rows * cellHeight);
        List<IconTargetMapping> mappings = new ArrayList<>();
        for (int i = 0; i < safeTargets.size(); i++) {
            IconTarget target = safeTargets.get(i);
            int row = i / safeColumns;
            int column = i % safeColumns;
            int cellLeft = column * cellWidth;
            int cellTop = row * cellHeight;
            IconBounds inputBounds = fitBoundsIntoCell(
                    target.bounds(),
                    cellLeft + padding,
                    cellTop + labelHeight + padding,
                    cellWidth - 2 * padding,
                    cellHeight - labelHeight - 2 * padding
            );
            mappings.add(new IconTargetMapping(
                    target.id(),
                    target.bounds(),
                    inputBounds,
                    target.id()
            ));
        }
        return new IconMontageLayout(
                width,
                height,
                safeColumns,
                cellWidth,
                cellHeight,
                labelHeight,
                mappings
        );
    }

    private static IconBounds fitBoundsIntoCell(
            IconBounds originalBounds,
            int areaLeft,
            int areaTop,
            int areaWidth,
            int areaHeight
    ) {
        int sourceWidth = originalBounds == null || !originalBounds.isValid()
                ? areaWidth
                : originalBounds.width();
        int sourceHeight = originalBounds == null || !originalBounds.isValid()
                ? areaHeight
                : originalBounds.height();
        float scale = Math.min(areaWidth / (float) sourceWidth, areaHeight / (float) sourceHeight);
        int targetWidth = Math.max(1, Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, Math.round(sourceHeight * scale));
        int left = areaLeft + Math.max(0, (areaWidth - targetWidth) / 2);
        int top = areaTop + Math.max(0, (areaHeight - targetHeight) / 2);
        return new IconBounds(left, top, left + targetWidth, top + targetHeight);
    }
}
