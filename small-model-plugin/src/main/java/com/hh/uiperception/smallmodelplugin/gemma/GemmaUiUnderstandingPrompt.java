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
