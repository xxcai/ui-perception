package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 拼接图布局结果。
 */
public final class IconMontageLayout {

    private final int width;
    private final int height;
    private final int columns;
    private final int cellWidth;
    private final int cellHeight;
    private final int labelHeight;
    private final List<IconTargetMapping> mappings;

    public IconMontageLayout(
            int width,
            int height,
            int columns,
            int cellWidth,
            int cellHeight,
            int labelHeight,
            List<IconTargetMapping> mappings
    ) {
        this.width = width;
        this.height = height;
        this.columns = columns;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.labelHeight = labelHeight;
        this.mappings = Collections.unmodifiableList(new ArrayList<>(
                mappings == null ? Collections.emptyList() : mappings
        ));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int columns() {
        return columns;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    public int labelHeight() {
        return labelHeight;
    }

    public List<IconTargetMapping> mappings() {
        return mappings;
    }
}
