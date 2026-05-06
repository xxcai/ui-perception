package com.hh.uiperception.evaluation;

import android.content.Context;

import com.hh.uiperception.core.PerceptionRunResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 手机端评测结果生成器。
 *
 * 第一版只扫描当前 run 目录中已经落盘的 artifact，并生成稳定 JSON。
 */
public final class OnDeviceEvaluationRunner {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";

    private OnDeviceEvaluationRunner() {
    }

    public static void generate(Context context, PerceptionRunResult runResult) {
        if (context == null || runResult == null) {
            return;
        }
        File baseDir = context.getExternalFilesDir(null);
        if (baseDir == null) {
            return;
        }
        File runDir = new File(baseDir,
                "captures/" + runResult.baselineId() + "/runs/" + runResult.runId());
        try {
            String targetsYaml = readTargets(context, runResult.baselineId());
            generate(runDir, runResult.baselineId(), runResult.runId(),
                    System.currentTimeMillis(), targetsYaml);
        } catch (IOException e) {
            // 手机端抓取主链路优先，评测文件生成失败暂不打断用户操作。
        }
    }

    static File generate(File runDir, String baselineId, String runId, long generatedAt)
            throws IOException {
        return generate(runDir, baselineId, runId, generatedAt, "");
    }

    static File generate(File runDir, String baselineId, String runId, long generatedAt,
                         String targetsYaml) throws IOException {
        List<EvaluationArtifact> artifacts = scanArtifacts(runDir);
        List<CountTarget> countTargets = CountTargetsParser.parse(targetsYaml);
        List<CountResult> countResults = evaluateCounts(runDir, artifacts, countTargets);
        File evaluationDir = new File(runDir, "evaluation");
        if (!evaluationDir.exists() && !evaluationDir.mkdirs()) {
            throw new IOException("创建评测目录失败: " + evaluationDir.getAbsolutePath());
        }
        File resultFile = new File(evaluationDir, "evaluation-result.json");
        try (FileWriter writer = new FileWriter(resultFile)) {
            writer.write(renderResult(baselineId, runId, generatedAt, artifacts, countResults));
        }
        return resultFile;
    }

    private static List<EvaluationArtifact> scanArtifacts(File runDir) {
        List<EvaluationArtifact> artifacts = new ArrayList<>();
        File[] pluginDirs = runDir.listFiles(File::isDirectory);
        if (pluginDirs == null) {
            return artifacts;
        }
        for (File pluginDir : pluginDirs) {
            if ("evaluation".equals(pluginDir.getName())) {
                continue;
            }
            scanArtifactType(runDir, pluginDir.getName(), new File(pluginDir, "raw"), artifacts);
            scanArtifactType(runDir, pluginDir.getName(), new File(pluginDir, "transformed"), artifacts);
        }
        artifacts.sort(Comparator
                .comparing(EvaluationArtifact::plugin)
                .thenComparing(EvaluationArtifact::path));
        return artifacts;
    }

    private static void scanArtifactType(File runDir, String plugin, File dir,
                                         List<EvaluationArtifact> artifacts) {
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            return;
        }
        for (File file : files) {
            String contentType = contentTypeFor(file.getName());
            String schemaStatus = contentType.isEmpty() || file.length() <= 0 ? STATUS_FAIL : STATUS_PASS;
            artifacts.add(new EvaluationArtifact(
                    artifactIdFor(file.getName(), contentType),
                    plugin,
                    relativePath(runDir, file),
                    contentType,
                    file.length(),
                    schemaStatus
            ));
        }
    }

    private static List<CountResult> evaluateCounts(
            File runDir,
            List<EvaluationArtifact> artifacts,
            List<CountTarget> targets) {
        File snapshotFile = findSemanticSnapshot(runDir, artifacts);
        List<SnapshotNode> nodes = snapshotFile != null
                ? SnapshotNodeParser.parse(readFileOrEmpty(snapshotFile)) : new ArrayList<>();
        List<CountResult> results = new ArrayList<>();
        for (CountTarget target : targets) {
            int count = 0;
            for (SnapshotNode node : nodes) {
                if (!target.role().isEmpty() && !target.role().equals(node.role())) {
                    continue;
                }
                if (!target.name().isEmpty() && !target.name().equals(node.name())) {
                    continue;
                }
                count++;
            }
            results.add(new CountResult(target, count,
                    count >= target.minCount() ? STATUS_PASS : STATUS_FAIL));
        }
        return results;
    }

    private static File findSemanticSnapshot(File runDir, List<EvaluationArtifact> artifacts) {
        for (EvaluationArtifact artifact : artifacts) {
            if ("native-semantic-snapshot".equals(artifact.id())
                    && STATUS_PASS.equals(artifact.schemaStatus())) {
                return new File(runDir, artifact.path());
            }
        }
        return null;
    }

    private static String renderResult(String baselineId, String runId, long generatedAt,
                                       List<EvaluationArtifact> artifacts,
                                       List<CountResult> countResults) {
        String overallStatus = artifacts.isEmpty() || hasFailedArtifact(artifacts)
                || hasFailedCount(countResults)
                ? STATUS_FAIL : STATUS_PASS;
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendStringField(builder, 1, "baselineId", baselineId, true);
        appendStringField(builder, 1, "runId", runId, true);
        appendNumberField(builder, 1, "generatedAt", generatedAt, true);
        appendCandidates(builder, artifacts);
        builder.append(",\n");
        appendCountResults(builder, countResults);
        builder.append(",\n");
        builder.append("  \"summary\": {\n");
        appendNumberField(builder, 2, "artifactCount", artifacts.size(), true);
        appendNumberField(builder, 2, "countTargetCount", countResults.size(), true);
        appendNumberField(builder, 2, "countTargetPassCount", countPassed(countResults), true);
        appendStringField(builder, 2, "status", overallStatus, false);
        builder.append("  }\n");
        builder.append("}\n");
        return builder.toString();
    }

    private static void appendCandidates(StringBuilder builder, List<EvaluationArtifact> artifacts) {
        builder.append("  \"candidates\": [");
        if (!artifacts.isEmpty()) {
            builder.append('\n');
            for (int i = 0; i < artifacts.size(); i++) {
                EvaluationArtifact artifact = artifacts.get(i);
                builder.append("    {\n");
                appendStringField(builder, 3, "id", artifact.id(), true);
                appendStringField(builder, 3, "plugin", artifact.plugin(), true);
                appendStringField(builder, 3, "path", artifact.path(), true);
                appendStringField(builder, 3, "contentType", artifact.contentType(), true);
                appendNumberField(builder, 3, "bytes", artifact.bytes(), true);
                appendStringField(builder, 3, "schemaStatus", artifact.schemaStatus(), false);
                builder.append("    }");
                if (i < artifacts.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            builder.append("  ");
        }
        builder.append(']');
    }

    private static void appendCountResults(StringBuilder builder, List<CountResult> results) {
        builder.append("  \"countResults\": [");
        if (!results.isEmpty()) {
            builder.append('\n');
            for (int i = 0; i < results.size(); i++) {
                CountResult result = results.get(i);
                builder.append("    {\n");
                appendStringField(builder, 3, "id", result.target().id(), true);
                appendStringField(builder, 3, "role", result.target().role(), true);
                appendStringField(builder, 3, "name", result.target().name(), true);
                appendNumberField(builder, 3, "minCount", result.target().minCount(), true);
                appendNumberField(builder, 3, "actualCount", result.actualCount(), true);
                appendStringField(builder, 3, "status", result.status(), false);
                builder.append("    }");
                if (i < results.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            builder.append("  ");
        }
        builder.append(']');
    }

    private static void appendStringField(StringBuilder builder, int indent, String key,
                                          String value, boolean comma) {
        appendIndent(builder, indent);
        builder.append('"').append(key).append("\": \"").append(escape(value)).append('"');
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private static void appendNumberField(StringBuilder builder, int indent, String key,
                                          long value, boolean comma) {
        appendIndent(builder, indent);
        builder.append('"').append(key).append("\": ").append(value);
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private static void appendIndent(StringBuilder builder, int indent) {
        builder.append("  ".repeat(indent));
    }

    private static boolean hasFailedArtifact(List<EvaluationArtifact> artifacts) {
        for (EvaluationArtifact artifact : artifacts) {
            if (!STATUS_PASS.equals(artifact.schemaStatus())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFailedCount(List<CountResult> results) {
        for (CountResult result : results) {
            if (!STATUS_PASS.equals(result.status())) {
                return true;
            }
        }
        return false;
    }

    private static long countPassed(List<CountResult> results) {
        long count = 0;
        for (CountResult result : results) {
            if (STATUS_PASS.equals(result.status())) {
                count++;
            }
        }
        return count;
    }

    private static String contentTypeFor(String filename) {
        if (filename.endsWith(".xml")) {
            return "text/xml";
        }
        if (filename.endsWith(".yml") || filename.endsWith(".yaml")) {
            return "text/yaml";
        }
        if (filename.endsWith(".json")) {
            return "application/json";
        }
        if (filename.endsWith(".html")) {
            return "text/html";
        }
        return "";
    }

    private static String artifactIdFor(String filename, String contentType) {
        if (filename.startsWith("native_xml_") && "text/xml".equals(contentType)) {
            return "native-raw-xml";
        }
        if (filename.startsWith("native_semantic_snapshot_") && "text/yaml".equals(contentType)) {
            return "native-semantic-snapshot";
        }
        String name = filename;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.replace('_', '-');
    }

    private static String relativePath(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    private static String readTargets(Context context, String baselineId) {
        String path = "evaluation-targets/" + baselineId + ".yml";
        try (InputStream in = context.getAssets().open(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String readFileOrEmpty(File file) {
        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class EvaluationArtifact {
        private final String id;
        private final String plugin;
        private final String path;
        private final String contentType;
        private final long bytes;
        private final String schemaStatus;

        EvaluationArtifact(String id, String plugin, String path, String contentType,
                           long bytes, String schemaStatus) {
            this.id = id;
            this.plugin = plugin;
            this.path = path;
            this.contentType = contentType;
            this.bytes = bytes;
            this.schemaStatus = schemaStatus;
        }

        String id() {
            return id;
        }

        String plugin() {
            return plugin;
        }

        String path() {
            return path;
        }

        String contentType() {
            return contentType;
        }

        long bytes() {
            return bytes;
        }

        String schemaStatus() {
            return schemaStatus;
        }
    }

    private static final class CountTarget {
        private final String id;
        private final String role;
        private final String name;
        private final int minCount;

        CountTarget(String id, String role, String name, int minCount) {
            this.id = id;
            this.role = role;
            this.name = name;
            this.minCount = minCount;
        }

        String id() {
            return id;
        }

        String role() {
            return role;
        }

        String name() {
            return name;
        }

        int minCount() {
            return minCount;
        }
    }

    private static final class CountResult {
        private final CountTarget target;
        private final int actualCount;
        private final String status;

        CountResult(CountTarget target, int actualCount, String status) {
            this.target = target;
            this.actualCount = actualCount;
            this.status = status;
        }

        CountTarget target() {
            return target;
        }

        int actualCount() {
            return actualCount;
        }

        String status() {
            return status;
        }
    }

    private static final class SnapshotNode {
        private final String role;
        private final String name;

        SnapshotNode(String role, String name) {
            this.role = role;
            this.name = name;
        }

        String role() {
            return role;
        }

        String name() {
            return name;
        }
    }

    private static final class SnapshotNodeParser {
        private SnapshotNodeParser() {
        }

        static List<SnapshotNode> parse(String snapshot) {
            List<SnapshotNode> nodes = new ArrayList<>();
            String[] lines = snapshot.split("\\r?\\n");
            for (String line : lines) {
                SnapshotNode node = parseLine(line);
                if (node != null) {
                    nodes.add(node);
                }
            }
            return nodes;
        }

        private static SnapshotNode parseLine(String line) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("- ")) {
                return null;
            }
            String content = trimmed.substring(2).trim();
            if (content.endsWith(":")) {
                content = content.substring(0, content.length() - 1).trim();
            }
            if (content.isEmpty()) {
                return null;
            }
            return new SnapshotNode(firstToken(content), quotedValue(content));
        }

        private static String firstToken(String content) {
            int end = content.length();
            int space = content.indexOf(' ');
            if (space >= 0) {
                end = Math.min(end, space);
            }
            int bracket = content.indexOf('[');
            if (bracket >= 0) {
                end = Math.min(end, bracket);
            }
            return content.substring(0, end);
        }

        private static String quotedValue(String content) {
            int start = content.indexOf('"');
            if (start < 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            boolean escaping = false;
            for (int i = start + 1; i < content.length(); i++) {
                char c = content.charAt(i);
                if (escaping) {
                    builder.append(c);
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    return builder.toString();
                } else {
                    builder.append(c);
                }
            }
            return "";
        }
    }

    private static final class CountTargetsParser {
        private CountTargetsParser() {
        }

        static List<CountTarget> parse(String yaml) {
            List<Map<String, String>> entries = SimpleYamlParser.parseSequence(yaml, "targets");
            List<CountTarget> targets = new ArrayList<>();
            for (Map<String, String> entry : entries) {
                String id = entry.get("id");
                String role = entry.get("role");
                String minCount = entry.get("minCount");
                if (id == null || id.isEmpty() || role == null || role.isEmpty()
                        || minCount == null || minCount.isEmpty()) {
                    continue;
                }
                targets.add(new CountTarget(
                        id,
                        role,
                        entry.getOrDefault("name", ""),
                        parseInt(minCount)
                ));
            }
            return targets;
        }

        private static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private static final class SimpleYamlParser {
        private SimpleYamlParser() {
        }

        static List<Map<String, String>> parseSequence(String yaml, String sequenceKey) {
            List<YamlLine> lines = parseLines(yaml);
            List<Map<String, String>> result = new ArrayList<>();
            int sequenceStart = -1;
            for (int i = 0; i < lines.size(); i++) {
                YamlLine line = lines.get(i);
                if (line.indent == 0 && sequenceKey.equals(line.key)) {
                    sequenceStart = i;
                    break;
                }
            }
            if (sequenceStart < 0) {
                return result;
            }

            Map<String, String> current = null;
            for (int i = sequenceStart + 1; i < lines.size(); i++) {
                YamlLine line = lines.get(i);
                if (line.indent == 0) {
                    break;
                }
                if (line.isListItem) {
                    if (current != null) {
                        result.add(current);
                    }
                    current = new LinkedHashMap<>();
                    if (line.key != null && !line.key.isEmpty()) {
                        current.put(line.key, line.value);
                    }
                } else if (current != null && line.key != null && !line.key.isEmpty()) {
                    current.put(line.key, line.value);
                }
            }
            if (current != null) {
                result.add(current);
            }
            return result;
        }

        private static List<YamlLine> parseLines(String yaml) {
            List<YamlLine> lines = new ArrayList<>();
            String[] rawLines = yaml.split("\\r?\\n");
            for (String rawLine : rawLines) {
                YamlLine line = parseLine(rawLine);
                if (line != null) {
                    lines.add(line);
                }
            }
            return lines;
        }

        private static YamlLine parseLine(String rawLine) {
            int comment = rawLine.indexOf('#');
            if (comment >= 0) {
                rawLine = rawLine.substring(0, comment);
            }
            int indent = 0;
            while (indent < rawLine.length() && rawLine.charAt(indent) == ' ') {
                indent++;
            }
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            boolean isListItem = trimmed.startsWith("- ");
            if (isListItem) {
                trimmed = trimmed.substring(2).trim();
            }
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                return new YamlLine(indent, null, "", isListItem);
            }
            String key = trimmed.substring(0, colon).trim();
            String value = unquote(trimmed.substring(colon + 1).trim());
            return new YamlLine(indent, key, value, isListItem);
        }

        private static String unquote(String value) {
            if (value.length() < 2) {
                return value;
            }
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }
    }

    private static final class YamlLine {
        private final int indent;
        private final String key;
        private final String value;
        private final boolean isListItem;

        YamlLine(int indent, String key, String value, boolean isListItem) {
            this.indent = indent;
            this.key = key;
            this.value = value;
            this.isListItem = isListItem;
        }
    }
}
