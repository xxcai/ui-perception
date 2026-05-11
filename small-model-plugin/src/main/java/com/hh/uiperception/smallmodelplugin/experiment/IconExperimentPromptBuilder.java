package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.List;

/**
 * 为不同输入方式生成绑定 prompt。
 */
public final class IconExperimentPromptBuilder {

    private IconExperimentPromptBuilder() {
    }

    public static String build(IconExperimentTestSet testSet, IconInputMode inputMode,
                               List<IconTargetMapping> mappings) {
        IconInputMode mode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        if (mode == IconInputMode.FULL_IMAGE_WITH_BOUNDS) {
            return fullImageWithBoundsPrompt(testSet);
        }
        if (mode == IconInputMode.CROPPED_MONTAGE) {
            return montagePrompt(mappings);
        }
        return fullImagePrompt(testSet);
    }

    public static String fullImagePrompt(IconExperimentTestSet testSet) {
        StringBuilder targets = new StringBuilder();
        for (IconTarget target : safeTargets(testSet)) {
            targets.append(target.id()).append("\n");
        }
        return "You are given a full mobile app screenshot.\n\n"
                + "Identify the visible app icons that correspond to the target ids below.\n"
                + "The target ids are labels for evaluation only. No coordinates are provided in this mode.\n\n"
                + "Targets:\n"
                + targets
                + "\nOutput exactly one line per target id.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese description>\n\n"
                + "If you cannot confidently match a target id to a visible icon, output:\n"
                + "<id>:unknown\n\n"
                + "No extra text.";
    }

    public static String fullImageWithBoundsPrompt(IconExperimentTestSet testSet) {
        StringBuilder targetRegions = new StringBuilder();
        for (IconTarget target : safeTargets(testSet)) {
            IconBounds bounds = target.bounds();
            if (bounds != null && bounds.isValid()) {
                targetRegions.append(target.id())
                        .append("=")
                        .append(bounds.left()).append(",")
                        .append(bounds.top()).append(",")
                        .append(bounds.right()).append(",")
                        .append(bounds.bottom()).append("\n");
            }
        }
        return "You are given a full mobile app screenshot and target icon regions.\n\n"
                + "Each target region is defined as:\n"
                + "<id>=<left>,<top>,<right>,<bottom>\n\n"
                + "For each target region:\n"
                + "- Inspect only pixels inside the region bounds.\n"
                + "- Describe the visible icon in short Chinese.\n"
                + "- If the region is unclear, output unknown.\n"
                + "- Do not use information outside the region.\n\n"
                + "Output exactly one line per target region.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese description>\n\n"
                + "No extra text.\n\n"
                + "Target regions:\n"
                + targetRegions;
    }

    public static String montagePrompt(List<IconTargetMapping> mappings) {
        StringBuilder targetLabels = new StringBuilder();
        if (mappings != null) {
            for (IconTargetMapping mapping : mappings) {
                targetLabels.append(mapping.label()).append("\n");
            }
        }
        return "You are given a montage image made from cropped mobile app icon regions.\n\n"
                + "Each crop has a visible id label above it.\n"
                + "Identify the icon inside each labeled crop.\n\n"
                + "Labels:\n"
                + targetLabels
                + "\nOutput exactly one line per label.\n"
                + "Use this exact format:\n"
                + "<id>:<short Chinese description>\n\n"
                + "If a crop is unclear, output:\n"
                + "<id>:unknown\n\n"
                + "No extra text.";
    }

    private static List<IconTarget> safeTargets(IconExperimentTestSet testSet) {
        return testSet == null ? java.util.Collections.emptyList() : testSet.targets();
    }
}
