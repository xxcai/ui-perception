package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析小模型输出的 <id>:<desc> 文本。
 */
public final class IconOutputParser {

    private IconOutputParser() {
    }

    public static List<ParsedIconDescription> parse(String rawOutput) {
        return parse(rawOutput, null);
    }

    public static List<ParsedIconDescription> parse(
            String rawOutput,
            List<IconTargetMapping> mappings
    ) {
        List<ParsedIconDescription> parsed = new ArrayList<>();
        if (rawOutput == null || rawOutput.trim().isEmpty()) {
            return parsed;
        }
        Map<String, String> labelToTargetId = buildLabelToTargetIdMap(mappings);
        String text = stripMarkdownFence(rawOutput);
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            ParsedIconDescription description = parseLine(line, labelToTargetId);
            if (description != null) {
                parsed.add(description);
            }
        }
        return parsed;
    }

    private static ParsedIconDescription parseLine(String line, Map<String, String> labelToTargetId) {
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
        String id = remapId(trimmed.substring(0, separator).trim(), labelToTargetId);
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

    private static Map<String, String> buildLabelToTargetIdMap(List<IconTargetMapping> mappings) {
        Map<String, String> labelToTargetId = new LinkedHashMap<>();
        if (mappings == null) {
            return labelToTargetId;
        }
        for (IconTargetMapping mapping : mappings) {
            if (mapping == null || mapping.label().isEmpty() || mapping.targetId().isEmpty()) {
                continue;
            }
            labelToTargetId.put(mapping.label(), mapping.targetId());
        }
        return labelToTargetId;
    }

    private static String remapId(String id, Map<String, String> labelToTargetId) {
        if (id == null || id.isEmpty() || labelToTargetId == null || labelToTargetId.isEmpty()) {
            return id == null ? "" : id;
        }
        String remapped = labelToTargetId.get(id);
        return remapped == null ? id : remapped;
    }
}
