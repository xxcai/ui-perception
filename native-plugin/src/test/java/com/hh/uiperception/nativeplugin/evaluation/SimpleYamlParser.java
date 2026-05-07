package com.hh.uiperception.nativeplugin.evaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 最小 YAML 解析器，仅支持 targets.yml 所需的子集：
 * - 顶层标量键值对（page: message）
 * - 一个命名序列键（targets:）下的扁平映射列表
 */
public final class SimpleYamlParser {

    private SimpleYamlParser() {
    }

    /**
     * 解析顶层标量键值对。
     * 例如 "page: message" → {"page": "message"}
     */
    public static Map<String, String> parseScalars(InputStream in) {
        Map<String, String> result = new LinkedHashMap<>();
        List<Line> lines = parseLines(in);
        for (Line line : lines) {
            if (line.indent == 0 && !line.isListItem && line.key != null && !line.key.isEmpty()) {
                result.put(line.key, line.value != null ? line.value : "");
            }
        }
        return result;
    }

    /**
     * 解析指定键下的序列条目，每个条目是一个扁平映射。
     * 例如：
     * targets:
     *   - id: search
     *     role: button
     *   - id: service
     *     role: button
     * → [{"id": "search", "role": "button"}, {"id": "service", "role": "button"}]
     */
    public static List<Map<String, String>> parseSequence(InputStream in, String sequenceKey) {
        List<Line> lines = parseLines(in);
        List<Map<String, String>> result = new ArrayList<>();

        // 找到序列键的位置
        int sequenceStart = -1;
        int sequenceIndent = -1;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (line.indent == 0 && sequenceKey.equals(line.key)) {
                sequenceStart = i;
                sequenceIndent = 0;
                break;
            }
        }
        if (sequenceStart < 0) {
            return result;
        }

        // 解析序列条目
        Map<String, String> currentEntry = null;
        for (int i = sequenceStart + 1; i < lines.size(); i++) {
            Line line = lines.get(i);

            // 如果回到顶层或更外层，序列结束
            if (line.indent <= sequenceIndent) {
                break;
            }

            // 序列项标记（如 "- id: search"）
            if (line.isListItem) {
                if (currentEntry != null) {
                    result.add(currentEntry);
                }
                currentEntry = new LinkedHashMap<>();
                if (line.key != null && !line.key.isEmpty()) {
                    currentEntry.put(line.key, line.value != null ? line.value : "");
                }
            } else if (currentEntry != null && line.key != null && !line.key.isEmpty()) {
                // 序列项的后续属性（如 "  role: button"）
                currentEntry.put(line.key, line.value != null ? line.value : "");
            }
        }

        if (currentEntry != null) {
            result.add(currentEntry);
        }

        return result;
    }

    private static List<Line> parseLines(InputStream in) {
        List<Line> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                Line parsed = parseLine(rawLine);
                if (parsed != null) {
                    lines.add(parsed);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 YAML 失败", e);
        }
        return lines;
    }

    private static Line parseLine(String rawLine) {
        // 去除尾部注释
        int commentIdx = rawLine.indexOf('#');
        if (commentIdx >= 0) {
            rawLine = rawLine.substring(0, commentIdx);
        }

        // 计算缩进
        int indent = 0;
        while (indent < rawLine.length() && rawLine.charAt(indent) == ' ') {
            indent++;
        }
        String trimmed = rawLine.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        // 判断是否为列表项
        boolean isListItem = trimmed.startsWith("- ");
        if (isListItem) {
            trimmed = trimmed.substring(2).trim();
        }

        // 解析键值对
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx < 0) {
            return new Line(indent, null, null, isListItem);
        }

        String key = trimmed.substring(0, colonIdx).trim();
        String value = trimmed.substring(colonIdx + 1).trim();

        // 去除引号包裹
        value = unquote(value);

        return new Line(indent, key, value.isEmpty() ? null : value, isListItem);
    }

    private static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static class Line {
        final int indent;
        final String key;
        final String value;
        final boolean isListItem;

        Line(int indent, String key, String value, boolean isListItem) {
            this.indent = indent;
            this.key = key;
            this.value = value;
            this.isListItem = isListItem;
        }
    }
}
