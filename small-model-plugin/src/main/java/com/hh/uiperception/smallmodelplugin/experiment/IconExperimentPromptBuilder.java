package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.List;

/**
 * 为不同输入方式生成绑定 prompt。
 */
public final class IconExperimentPromptBuilder {

    private IconExperimentPromptBuilder() {
    }

    public static String build(IconInputMode inputMode, List<IconTargetMapping> mappings) {
        IconInputMode mode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        if (mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS
                || mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS_BATCHED) {
            if (mode == IconInputMode.FULL_IMAGE_WITH_MARKED_BOUNDS) {
                return markedBoundsPrompt(mappings);
            }
            return fullImageWithBoundsPrompt(mappings);
        }
        if (mode == IconInputMode.CROPPED_MONTAGE) {
            return montagePrompt(mappings);
        }
        return fullImagePrompt(mappings);
    }

    public static String fullImagePrompt(List<IconTargetMapping> mappings) {
        StringBuilder targets = new StringBuilder();
        int targetCount = 0;
        for (IconTargetMapping mapping : safeMappings(mappings)) {
            targets.append(mapping.label()).append("\n");
            targetCount++;
        }
        return "You are given a full mobile app screenshot.\n\n"
                + "Identify the visible app icons that correspond to the target ids below.\n"
                + "The target ids are labels for evaluation only. No coordinates are provided in this mode.\n\n"
                + "Targets:\n"
                + targets
                + "\nOutput exactly one line per target id.\n"
                + "Write every description in Simplified Chinese. Do not use English except the id and unknown.\n"
                + "Use a very short label of 1 to 4 Chinese characters only.\n"
                + "Do not write sentences, explanations, modifiers, or punctuation after the label.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese label>\n\n"
                + "If you cannot confidently match a target id to a visible icon, output:\n"
                + "<id>:unknown\n\n"
                + "Return exactly "
                + targetCount
                + " lines and stop immediately after the last line.\n"
                + "No extra text.";
    }

    public static String fullImageWithBoundsPrompt(List<IconTargetMapping> mappings) {
        return fullImageWithBoundsPrompt(mappings, 0, 0);
    }

    public static String fullImageWithBoundsPrompt(
            List<IconTargetMapping> mappings,
            int imageWidth,
            int imageHeight
    ) {
        StringBuilder targetRegions = new StringBuilder();
        int maxRight = 0;
        int maxBottom = 0;
        int targetCount = 0;
        for (IconTargetMapping mapping : safeMappings(mappings)) {
            IconBounds bounds = mapping.originalBounds();
            if (bounds != null && bounds.isValid()) {
                maxRight = Math.max(maxRight, bounds.right());
                maxBottom = Math.max(maxBottom, bounds.bottom());
                targetCount++;
                targetRegions.append(mapping.label())
                        .append("=")
                        .append(bounds.left()).append(",")
                        .append(bounds.top()).append(",")
                        .append(bounds.right()).append(",")
                        .append(bounds.bottom()).append("\n");
            }
        }
        int describedWidth = imageWidth > 0 ? imageWidth : maxRight;
        int describedHeight = imageHeight > 0 ? imageHeight : maxBottom;
        return "You are given one full mobile app screenshot and a list of target regions.\n\n"
                + "Coordinate system:\n"
                + "- Coordinates are pixel coordinates in the original screenshot.\n"
                + "- The screenshot size is "
                + describedWidth
                + " pixels wide and "
                + describedHeight
                + " pixels high.\n"
                + "- Valid x range is 0 to "
                + describedWidth
                + ". Valid y range is 0 to "
                + describedHeight
                + ".\n"
                + "- The origin (0,0) is the top-left corner of the screenshot.\n"
                + "- x increases from left to right. y increases from top to bottom.\n"
                + "- A region is a rectangle: left edge x, top edge y, right edge x, bottom edge y.\n"
                + "- Only inspect pixels inside each rectangle. Ignore all pixels outside that rectangle.\n\n"
                + "Each target region is defined as:\n"
                + "<id>=<left>,<top>,<right>,<bottom>\n\n"
                + "Task:\n"
                + "Process the target regions one by one in the exact order listed below.\n"
                + "For each id, look at only that rectangle and describe the icon or visual symbol inside it in short Chinese.\n"
                + "Do not infer from nearby text, surrounding list items, or other regions.\n"
                + "If a rectangle does not clearly contain an icon or visual symbol, output unknown.\n\n"
                + "Output exactly one line per target region.\n"
                + "Write every description in Simplified Chinese. Do not use English except the id and unknown.\n"
                + "Use a very short label of 1 to 4 Chinese characters only.\n"
                + "Do not write sentences, explanations, modifiers, or punctuation after the label.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese label>\n\n"
                + "Return exactly "
                + targetCount
                + " lines and stop immediately after the last line.\n"
                + "No extra text.\n\n"
                + "Target regions:\n"
                + targetRegions;
    }

    public static String montagePrompt(List<IconTargetMapping> mappings) {
        StringBuilder targetLabels = new StringBuilder();
        int targetCount = 0;
        if (mappings != null) {
            for (IconTargetMapping mapping : mappings) {
                targetLabels.append(mapping.label()).append("\n");
                targetCount++;
            }
        }
        return "You are given a montage image made from cropped mobile app icon regions.\n\n"
                + "Each crop has a visible id label above it.\n"
                + "Identify the icon inside each labeled crop.\n\n"
                + "Labels:\n"
                + targetLabels
                + "\nOutput exactly one line per label.\n"
                + "Write every description in Simplified Chinese. Do not use English except the id and unknown.\n"
                + "Use a very short label of 1 to 4 Chinese characters only.\n"
                + "Do not write sentences, explanations, modifiers, or punctuation after the label.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese label>\n\n"
                + "If a crop is unclear, output:\n"
                + "<id>:unknown\n\n"
                + "Return exactly "
                + targetCount
                + " lines and stop immediately after the last line.\n"
                + "No extra text.";
    }

    public static String markedBoundsPrompt(List<IconTargetMapping> mappings) {
        StringBuilder targetLabels = new StringBuilder();
        int targetCount = 0;
        for (IconTargetMapping mapping : safeMappings(mappings)) {
            targetLabels.append(mapping.label()).append("\n");
            targetCount++;
        }
        return "You are given a mobile app screenshot with visible blue rectangles and id labels.\n\n"
                + "Task:\n"
                + "For each target id below, inspect only the icon or visual symbol inside the blue rectangle with that id label.\n"
                + "Use the visible rectangle and label in the image. Do not use pixel coordinates.\n"
                + "Ignore nearby text and UI outside the rectangle.\n"
                + "If a rectangle does not clearly contain an icon or visual symbol, output unknown.\n\n"
                + "Target ids:\n"
                + targetLabels
                + "\nOutput exactly one line per target id.\n"
                + "Write every description in Simplified Chinese. Do not use English except the id and unknown.\n"
                + "Use a very short label of 1 to 4 Chinese characters only.\n"
                + "Do not write sentences, explanations, modifiers, or punctuation after the label.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese label>\n\n"
                + "Return exactly "
                + targetCount
                + " lines and stop immediately after the last line.\n"
                + "No extra text.";
    }

    private static List<IconTargetMapping> safeMappings(List<IconTargetMapping> mappings) {
        return mappings == null ? java.util.Collections.emptyList() : mappings;
    }
}
