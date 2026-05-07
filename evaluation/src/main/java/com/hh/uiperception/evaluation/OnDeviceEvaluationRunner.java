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
import java.util.Locale;
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
        List<EvidenceSource> evidenceSources = adaptEvidenceSources(runDir, artifacts);
        List<CountTarget> countTargets = CountTargetsParser.parse(targetsYaml);
        List<InformationTarget> informationTargets = InformationTargetsParser.parse(targetsYaml);
        List<CountResult> countResults = evaluateCounts(evidenceSources, countTargets);
        List<TargetResult> targetResults = evaluateInformationTargets(
                evidenceSources, informationTargets);
        File evaluationDir = new File(runDir, "evaluation");
        if (!evaluationDir.exists() && !evaluationDir.mkdirs()) {
            throw new IOException("创建评测目录失败: " + evaluationDir.getAbsolutePath());
        }
        File resultFile = new File(evaluationDir, "evaluation-result.json");
        try (FileWriter writer = new FileWriter(resultFile)) {
            writer.write(renderResult(baselineId, runId, generatedAt, artifacts,
                    countResults, targetResults));
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
        String stage = dir.getName();
        for (File file : files) {
            String contentType = contentTypeFor(file.getName());
            String schemaStatus = contentType.isEmpty() || file.length() <= 0 ? STATUS_FAIL : STATUS_PASS;
            String type = artifactTypeFor(plugin, stage, file.getName(), contentType);
            artifacts.add(new EvaluationArtifact(
                    artifactIdFor(plugin, type, file.getName()),
                    plugin,
                    stage,
                    type,
                    relativePath(runDir, file),
                    contentType,
                    file.length(),
                    schemaStatus
            ));
        }
    }

    private static List<EvidenceSource> adaptEvidenceSources(
            File runDir,
            List<EvaluationArtifact> artifacts) {
        List<EvidenceSource> sources = new ArrayList<>();
        for (EvaluationArtifact artifact : artifacts) {
            if (!STATUS_PASS.equals(artifact.schemaStatus())) {
                continue;
            }
            if ("llm_input".equals(artifact.type())) {
                File file = new File(runDir, artifact.path());
                sources.add(new EvidenceSource(
                        artifact.id(),
                        artifact.plugin(),
                        artifact.type(),
                        SnapshotNodeParser.parse(readFileOrEmpty(file))
                ));
            }
        }
        return sources;
    }

    private static List<CountResult> evaluateCounts(
            List<EvidenceSource> sources,
            List<CountTarget> targets) {
        List<SnapshotNode> nodes = collectNodes(sources);
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

    private static List<TargetResult> evaluateInformationTargets(
            List<EvidenceSource> sources,
            List<InformationTarget> targets) {
        List<SnapshotNode> nodes = collectNodes(sources);
        List<TargetResult> results = new ArrayList<>();
        for (InformationTarget target : targets) {
            List<EvidenceResult> evidenceResults = new ArrayList<>();
            for (CountTarget evidence : target.evidence()) {
                int count = countMatches(nodes, evidence);
                evidenceResults.add(new EvidenceResult(evidence, count,
                        count >= evidence.minCount() ? STATUS_PASS : STATUS_FAIL));
            }
            results.add(new TargetResult(target, evidenceResults,
                    hasFailedEvidence(evidenceResults) ? STATUS_FAIL : STATUS_PASS));
        }
        return results;
    }

    private static List<SnapshotNode> collectNodes(List<EvidenceSource> sources) {
        List<SnapshotNode> nodes = new ArrayList<>();
        for (EvidenceSource source : sources) {
            nodes.addAll(source.nodes());
        }
        return nodes;
    }

    private static int countMatches(List<SnapshotNode> nodes, CountTarget target) {
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
        return count;
    }

    private static String renderResult(String baselineId, String runId, long generatedAt,
                                       List<EvaluationArtifact> artifacts,
                                       List<CountResult> countResults,
                                       List<TargetResult> targetResults) {
        String overallStatus = artifacts.isEmpty() || hasFailedArtifact(artifacts)
                || hasFailedCount(countResults)
                || hasFailedTarget(targetResults)
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
        appendTargetResults(builder, targetResults);
        builder.append(",\n");
        builder.append("  \"summary\": {\n");
        appendNumberField(builder, 2, "artifactCount", artifacts.size(), true);
        appendNumberField(builder, 2, "countTargetCount", countResults.size(), true);
        appendNumberField(builder, 2, "countTargetPassCount", countPassed(countResults), true);
        appendNumberField(builder, 2, "targetCount", targetResults.size(), true);
        appendNumberField(builder, 2, "targetPassCount", targetPassed(targetResults), true);
        appendNumberField(builder, 2, "evidenceCount", evidenceCount(targetResults), true);
        appendNumberField(builder, 2, "evidencePassCount", evidencePassed(targetResults), true);
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
                appendStringField(builder, 3, "stage", artifact.stage(), true);
                appendStringField(builder, 3, "type", artifact.type(), true);
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

    private static void appendTargetResults(StringBuilder builder, List<TargetResult> results) {
        builder.append("  \"targetResults\": [");
        if (!results.isEmpty()) {
            builder.append('\n');
            for (int i = 0; i < results.size(); i++) {
                TargetResult result = results.get(i);
                builder.append("    {\n");
                appendStringField(builder, 3, "id", result.target().id(), true);
                appendStringField(builder, 3, "type", result.target().type(), true);
                appendStringField(builder, 3, "description", result.target().description(), true);
                appendNumberField(builder, 3, "passedEvidence", evidencePassed(result), true);
                appendNumberField(builder, 3, "totalEvidence", result.evidenceResults().size(), true);
                appendDecimalField(builder, 3, "score", score(result), true);
                appendEvidenceResults(builder, result.evidenceResults());
                builder.append(",\n");
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

    private static void appendEvidenceResults(StringBuilder builder, List<EvidenceResult> results) {
        appendIndent(builder, 3);
        builder.append("\"evidenceResults\": [");
        if (!results.isEmpty()) {
            builder.append('\n');
            for (int i = 0; i < results.size(); i++) {
                EvidenceResult result = results.get(i);
                builder.append("        {\n");
                appendStringField(builder, 5, "id", result.evidence().id(), true);
                appendStringField(builder, 5, "capability", result.evidence().capability(), true);
                appendStringField(builder, 5, "role", result.evidence().role(), true);
                appendStringField(builder, 5, "name", result.evidence().name(), true);
                appendNumberField(builder, 5, "minCount", result.evidence().minCount(), true);
                appendNumberField(builder, 5, "actualCount", result.actualCount(), true);
                appendStringField(builder, 5, "status", result.status(), false);
                builder.append("        }");
                if (i < results.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            appendIndent(builder, 3);
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

    private static void appendDecimalField(StringBuilder builder, int indent, String key,
                                           double value, boolean comma) {
        appendIndent(builder, indent);
        builder.append('"').append(key).append("\": ")
                .append(String.format(Locale.US, "%.2f", value));
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

    private static boolean hasFailedTarget(List<TargetResult> results) {
        for (TargetResult result : results) {
            if (!STATUS_PASS.equals(result.status())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFailedEvidence(List<EvidenceResult> results) {
        for (EvidenceResult result : results) {
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

    private static long targetPassed(List<TargetResult> results) {
        long count = 0;
        for (TargetResult result : results) {
            if (STATUS_PASS.equals(result.status())) {
                count++;
            }
        }
        return count;
    }

    private static long evidenceCount(List<TargetResult> results) {
        long count = 0;
        for (TargetResult result : results) {
            count += result.evidenceResults().size();
        }
        return count;
    }

    private static long evidencePassed(List<TargetResult> results) {
        long count = 0;
        for (TargetResult result : results) {
            count += evidencePassed(result);
        }
        return count;
    }

    private static long evidencePassed(TargetResult result) {
        long count = 0;
        for (EvidenceResult evidenceResult : result.evidenceResults()) {
            if (STATUS_PASS.equals(evidenceResult.status())) {
                count++;
            }
        }
        return count;
    }

    private static double score(TargetResult result) {
        if (result.evidenceResults().isEmpty()) {
            return 0.0;
        }
        return (double) evidencePassed(result) / result.evidenceResults().size();
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

    private static String artifactTypeFor(String plugin, String stage, String filename,
                                          String contentType) {
        if ("native".equals(plugin) && "raw".equals(stage)
                && filename.startsWith("native_xml_") && "text/xml".equals(contentType)) {
            return "raw_xml";
        }
        if ("native".equals(plugin) && "transformed".equals(stage)
                && filename.startsWith("native_semantic_snapshot_")
                && "text/yaml".equals(contentType)) {
            return "llm_input";
        }
        if ("transformed".equals(stage)
                && filename.startsWith("llm_input_")
                && "text/yaml".equals(contentType)) {
            return "llm_input";
        }
        if ("text/xml".equals(contentType)) {
            return "xml";
        }
        if ("text/yaml".equals(contentType)) {
            return "yaml";
        }
        if ("application/json".equals(contentType)) {
            return "json";
        }
        if ("text/html".equals(contentType)) {
            return "html";
        }
        String name = filename;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.replace('_', '-');
    }

    private static String artifactIdFor(String plugin, String type, String filename) {
        if (type == null || type.isEmpty()) {
            String name = filename;
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
            type = name.replace('_', '-');
        }
        return plugin + "." + type;
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
        private final String stage;
        private final String type;
        private final String path;
        private final String contentType;
        private final long bytes;
        private final String schemaStatus;

        EvaluationArtifact(String id, String plugin, String stage, String type, String path,
                           String contentType, long bytes, String schemaStatus) {
            this.id = id;
            this.plugin = plugin;
            this.stage = stage;
            this.type = type;
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

        String stage() {
            return stage;
        }

        String type() {
            return type;
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

    private static final class EvidenceSource {
        private final String candidateId;
        private final String plugin;
        private final String sourceType;
        private final List<SnapshotNode> nodes;

        EvidenceSource(String candidateId, String plugin, String sourceType,
                       List<SnapshotNode> nodes) {
            this.candidateId = candidateId;
            this.plugin = plugin;
            this.sourceType = sourceType;
            this.nodes = nodes;
        }

        String candidateId() {
            return candidateId;
        }

        String plugin() {
            return plugin;
        }

        String sourceType() {
            return sourceType;
        }

        List<SnapshotNode> nodes() {
            return nodes;
        }
    }

    private static final class CountTarget {
        private final String id;
        private final String capability;
        private final String role;
        private final String name;
        private final int minCount;

        CountTarget(String id, String capability, String role, String name, int minCount) {
            this.id = id;
            this.capability = capability;
            this.role = role;
            this.name = name;
            this.minCount = minCount;
        }

        String id() {
            return id;
        }

        String capability() {
            return capability;
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

    private static final class InformationTarget {
        private final String id;
        private final String type;
        private final String description;
        private final List<CountTarget> evidence;

        InformationTarget(String id, String type, String description, List<CountTarget> evidence) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.evidence = evidence;
        }

        String id() {
            return id;
        }

        String type() {
            return type;
        }

        String description() {
            return description;
        }

        List<CountTarget> evidence() {
            return evidence;
        }
    }

    private static final class EvidenceResult {
        private final CountTarget evidence;
        private final int actualCount;
        private final String status;

        EvidenceResult(CountTarget evidence, int actualCount, String status) {
            this.evidence = evidence;
            this.actualCount = actualCount;
            this.status = status;
        }

        CountTarget evidence() {
            return evidence;
        }

        int actualCount() {
            return actualCount;
        }

        String status() {
            return status;
        }
    }

    private static final class TargetResult {
        private final InformationTarget target;
        private final List<EvidenceResult> evidenceResults;
        private final String status;

        TargetResult(InformationTarget target, List<EvidenceResult> evidenceResults,
                     String status) {
            this.target = target;
            this.evidenceResults = evidenceResults;
            this.status = status;
        }

        InformationTarget target() {
            return target;
        }

        List<EvidenceResult> evidenceResults() {
            return evidenceResults;
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
            String minCount = entry.getOrDefault("minCount", "1");
            if (id == null || id.isEmpty() || role == null || role.isEmpty()
                    || minCount.isEmpty()) {
                continue;
            }
                targets.add(new CountTarget(
                        id,
                        entry.getOrDefault("capability", ""),
                        role,
                        entry.getOrDefault("name", ""),
                        parseInt(minCount)
                ));
            }
            return targets;
        }

        static int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private static final class InformationTargetsParser {
        private InformationTargetsParser() {
        }

        static List<InformationTarget> parse(String yaml) {
            List<YamlLine> lines = SimpleYamlParser.parseLines(yaml);
            List<InformationTarget> targets = new ArrayList<>();
            int sequenceStart = findSequenceStart(lines, "targets");
            if (sequenceStart < 0) {
                return targets;
            }

            int sequenceIndent = lines.get(sequenceStart).indent;
            int targetIndent = -1;
            Map<String, String> currentTarget = null;
            Map<String, String> currentEvidence = null;
            List<CountTarget> currentEvidenceList = new ArrayList<>();
            int evidenceIndent = -1;
            boolean inEvidence = false;

            for (int i = sequenceStart + 1; i < lines.size(); i++) {
                YamlLine line = lines.get(i);
                if (line.indent <= sequenceIndent) {
                    break;
                }
                if (line.isListItem && (targetIndent < 0 || line.indent == targetIndent)) {
                    if (targetIndent < 0) {
                        targetIndent = line.indent;
                    }
                    addEvidence(currentEvidenceList, currentEvidence);
                    addInformationTarget(targets, currentTarget, currentEvidenceList);
                    currentTarget = new LinkedHashMap<>();
                    currentEvidence = null;
                    currentEvidenceList = new ArrayList<>();
                    evidenceIndent = -1;
                    inEvidence = false;
                    if (line.key != null && !line.key.isEmpty()) {
                        currentTarget.put(line.key, line.value);
                    }
                    continue;
                }
                if (currentTarget == null) {
                    continue;
                }
                if ("evidence".equals(line.key) && !line.isListItem) {
                    inEvidence = true;
                    currentEvidence = null;
                    evidenceIndent = -1;
                    continue;
                }
                if (inEvidence && line.isListItem && line.indent > targetIndent) {
                    addEvidence(currentEvidenceList, currentEvidence);
                    currentEvidence = new LinkedHashMap<>();
                    evidenceIndent = line.indent;
                    if (line.key != null && !line.key.isEmpty()) {
                        currentEvidence.put(line.key, line.value);
                    }
                    continue;
                }
                if (currentEvidence != null && line.indent > evidenceIndent
                        && line.key != null && !line.key.isEmpty()) {
                    currentEvidence.put(line.key, line.value);
                } else if (!inEvidence && line.indent > targetIndent
                        && line.key != null && !line.key.isEmpty()) {
                    currentTarget.put(line.key, line.value);
                }
            }
            addEvidence(currentEvidenceList, currentEvidence);
            addInformationTarget(targets, currentTarget, currentEvidenceList);
            return targets;
        }

        private static int findSequenceStart(List<YamlLine> lines, String sequenceKey) {
            for (int i = 0; i < lines.size(); i++) {
                YamlLine line = lines.get(i);
                if (line.indent == 0 && sequenceKey.equals(line.key)) {
                    return i;
                }
            }
            return -1;
        }

        private static void addInformationTarget(List<InformationTarget> targets,
                                                 Map<String, String> entry,
                                                 List<CountTarget> evidence) {
            if (entry == null || evidence.isEmpty()) {
                return;
            }
            String id = entry.get("id");
            if (id == null || id.isEmpty()) {
                return;
            }
            targets.add(new InformationTarget(
                    id,
                    entry.getOrDefault("type", "information"),
                    entry.getOrDefault("description", ""),
                    evidence
            ));
        }

        private static void addEvidence(List<CountTarget> evidence,
                                        Map<String, String> entry) {
            if (entry == null) {
                return;
            }
            CountTarget target = countTargetFrom(entry);
            if (target != null) {
                evidence.add(target);
            }
        }

        private static CountTarget countTargetFrom(Map<String, String> entry) {
            String id = entry.get("id");
            String role = entry.get("role");
            String minCount = entry.getOrDefault("minCount", "1");
            if (id == null || id.isEmpty() || role == null || role.isEmpty()
                    || minCount.isEmpty()) {
                return null;
            }
            return new CountTarget(
                    id,
                    entry.getOrDefault("capability", ""),
                    role,
                    entry.getOrDefault("name", ""),
                    CountTargetsParser.parseInt(minCount)
            );
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
            int sequenceIndent = lines.get(sequenceStart).indent;
            int itemIndent = -1;
            int fieldIndent = -1;
            for (int i = sequenceStart + 1; i < lines.size(); i++) {
                YamlLine line = lines.get(i);
                if (line.indent <= sequenceIndent) {
                    break;
                }
                if (line.isListItem && (itemIndent < 0 || line.indent == itemIndent)) {
                    if (itemIndent < 0) {
                        itemIndent = line.indent;
                    }
                    fieldIndent = -1;
                    if (current != null) {
                        result.add(current);
                    }
                    current = new LinkedHashMap<>();
                    if (line.key != null && !line.key.isEmpty()) {
                        current.put(line.key, line.value);
                    }
                } else if (current != null && itemIndent >= 0 && line.indent > itemIndent
                        && !line.isListItem && line.key != null && !line.key.isEmpty()) {
                    if (fieldIndent < 0) {
                        fieldIndent = line.indent;
                    }
                    if (line.indent == fieldIndent) {
                        current.put(line.key, line.value);
                    }
                }
            }
            if (current != null) {
                result.add(current);
            }
            return result;
        }

        static List<YamlLine> parseLines(String yaml) {
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
