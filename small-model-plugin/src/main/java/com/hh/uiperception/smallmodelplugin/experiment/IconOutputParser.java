package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析小模型输出的 <id>:<desc> 文本。
 */
public final class IconOutputParser {

    private IconOutputParser() {
    }

    public static List<ParsedIconDescription> parse(String rawOutput) {
        List<ParsedIconDescription> parsed = new ArrayList<>();
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return parsed;
        }
        String text = stripMarkdownFence(rawOutput);
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            ParsedIconDescription description = parseLine(line);
            if (description != null) {
                parsed.add(description);
            }
        }
        return parsed;
    }

    private static ParsedIconDescription parseLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            separator = trimmed.indexOf('：');
        }
        if (separator <= 0 || separator >= trimmed.length() - 1) {
            return null;
        }
        String id = trimmed.substring(0, separator).trim();
        String desc = trimmed.substring(separator + 1).trim();
        if (id.isEmpty() || desc.isEmpty()) {
            return null;
        }
        return new ParsedIconDescription(id, desc);
    }

    private static String stripMarkdownFence(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }
}
