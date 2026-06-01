package com.hh.uiperception.smallmodelplugin.experiment;

/**
 * target 在原图和当前模型输入图中的坐标映射。
 */
public final class IconTargetMapping {

    private final String targetId;
    private final IconBounds originalBounds;
    private final IconBounds inputBounds;
    private final String label;

    public IconTargetMapping(
            String targetId,
            IconBounds originalBounds,
            IconBounds inputBounds,
            String label
    ) {
        this.targetId = normalize(targetId);
        this.originalBounds = originalBounds;
        this.inputBounds = inputBounds;
        this.label = normalize(label);
    }

    public String targetId() {
        return targetId;
    }

    public IconBounds originalBounds() {
        return originalBounds;
    }

    public IconBounds inputBounds() {
        return inputBounds;
    }

    public String label() {
        return label;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
