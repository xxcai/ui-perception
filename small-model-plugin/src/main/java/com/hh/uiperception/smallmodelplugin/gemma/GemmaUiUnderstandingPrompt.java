package com.hh.uiperception.smallmodelplugin.gemma;

/**
 * Gemma UI 理解默认 prompt。
 */
public final class GemmaUiUnderstandingPrompt {

    private GemmaUiUnderstandingPrompt() {
    }

    public static String defaultPrompt() {
        return "You are a mobile UI perception model.\n"
                + "Analyze the screenshot and return only YAML lines in LLM Input Snapshot v1.\n"
                + "Allowed roles: text, button, list, listitem, image, visual_state.\n"
                + "Format examples:\n"
                + "- text \"消息\"\n"
                + "- button \"搜索\" [ref=s1]\n"
                + "- listitem [ref=s2]:\n"
                + "  - text \"平台通知\"\n"
                + "Rules:\n"
                + "- Use Chinese text exactly as visible.\n"
                + "- Do not invent invisible content.\n"
                + "- Use button only when the element is visually actionable.\n"
                + "- Return YAML only, no Markdown fence, no explanation.";
    }

    public static String compactPrompt() {
        return "Analyze this mobile UI screenshot. Return YAML only.\n"
                + "List visible text and actionable controls. Use this format:\n"
                + "- text \"visible text\"\n"
                + "- button \"visible button\" [ref=s1]\n"
                + "- listitem [ref=s2]:\n"
                + "  - text \"item text\"\n"
                + "Keep only important visible content. Do not explain.";
    }

    public static String minimalPrompt() {
        return "Return YAML only. Extract visible mobile UI text and buttons:\n"
                + "- text \"...\"\n"
                + "- button \"...\" [ref=s1]\n"
                + "No explanation.";
    }

    public static String refDescriptionPromptAll() {
        return refDescriptionPrompt("n1", "n2");
    }

    public static String refDescriptionPromptN1() {
        return refDescriptionPrompt("n1");
    }

    public static String refDescriptionPromptN2() {
        return refDescriptionPrompt("n2");
    }

    public static String refDescriptionPromptTopIcons() {
        return refDescriptionPrompt(
                new String[]{"icon1", "828,168,933,273"},
                new String[]{"icon2", "933,168,1038,273"}
        );
    }

    private static String refDescriptionPrompt(String... refs) {
        String[][] regions = new String[refs.length][2];
        for (int i = 0; i < refs.length; i++) {
            String ref = refs[i];
            if ("n1".equals(ref)) {
                regions[i] = new String[]{"n1", "0,436,1080,541"};
            } else if ("n2".equals(ref)) {
                regions[i] = new String[]{"n2", "0,636,1080,2190"};
            } else {
                regions[i] = new String[]{ref, ""};
            }
        }
        return refDescriptionPrompt(regions);
    }

    private static String refDescriptionPrompt(String[]... regions) {
        StringBuilder targetRegions = new StringBuilder();
        for (String[] region : regions) {
            if (region.length >= 2 && !region[0].isEmpty() && !region[1].isEmpty()) {
                targetRegions.append(region[0]).append("=").append(region[1]).append("\n");
            }
        }
        return refDescriptionPromptText(targetRegions.toString());
    }

    private static String refDescriptionPromptText(String targetRegions) {
        return "You are given a full mobile screenshot and target regions.\n\n"
                + "Each target region is defined as:\n"
                + "<ref_id>=<left>,<top>,<right>,<bottom>\n\n"
                + "For each target region:\n"
                + "- Inspect only pixels inside the region bounds.\n"
                + "- Output a short Chinese description of the visible UI content inside that region.\n"
                + "- If the region contains only an icon, describe the icon visually.\n"
                + "- If the content is unclear, output unknown.\n"
                + "- Do not use information outside the region.\n"
                + "- Do not infer the region purpose from nearby UI.\n\n"
                + "Output exactly one line per target region.\n"
                + "Use this exact format:\n"
                + "<ref_id>:<description>\n\n"
                + "No extra text.\n\n"
                + "Target regions:\n"
                + targetRegions;
    }

    public static String rawTextToYamlCandidate(String rawText) {
        if (rawText == null) {
            return "";
        }
        String text = rawText.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }
}
