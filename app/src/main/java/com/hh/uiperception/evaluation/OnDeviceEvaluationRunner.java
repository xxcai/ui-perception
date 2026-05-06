package com.hh.uiperception.evaluation;

import android.content.Context;

import com.hh.uiperception.core.PerceptionRunResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            generate(runDir, runResult.baselineId(), runResult.runId(), System.currentTimeMillis());
        } catch (IOException e) {
            // 手机端抓取主链路优先，评测文件生成失败暂不打断用户操作。
        }
    }

    static File generate(File runDir, String baselineId, String runId, long generatedAt)
            throws IOException {
        List<EvaluationArtifact> artifacts = scanArtifacts(runDir);
        File evaluationDir = new File(runDir, "evaluation");
        if (!evaluationDir.exists() && !evaluationDir.mkdirs()) {
            throw new IOException("创建评测目录失败: " + evaluationDir.getAbsolutePath());
        }
        File resultFile = new File(evaluationDir, "evaluation-result.json");
        try (FileWriter writer = new FileWriter(resultFile)) {
            writer.write(renderResult(baselineId, runId, generatedAt, artifacts));
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

    private static String renderResult(String baselineId, String runId, long generatedAt,
                                       List<EvaluationArtifact> artifacts) {
        String overallStatus = artifacts.isEmpty() || hasFailedArtifact(artifacts)
                ? STATUS_FAIL : STATUS_PASS;
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendStringField(builder, 1, "baselineId", baselineId, true);
        appendStringField(builder, 1, "runId", runId, true);
        appendNumberField(builder, 1, "generatedAt", generatedAt, true);
        appendCandidates(builder, artifacts);
        builder.append(",\n");
        builder.append("  \"summary\": {\n");
        appendNumberField(builder, 2, "artifactCount", artifacts.size(), true);
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
}
