package com.hh.uiperception.evaluation;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class OnDeviceEvaluationRunnerTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writesEvaluationResultForRunArtifacts() throws Exception {
        File runDir = temporaryFolder.newFolder("run");
        write(new File(runDir, "native/raw/native_xml_100.xml"), "<hierarchy />");
        write(new File(runDir, "native/transformed/native_semantic_snapshot_101.yml"),
                "- text \"消息\"");

        File result = OnDeviceEvaluationRunner.generate(
                runDir, "native_home_message", "100", 1234L);

        String json = read(result);
        assertTrue(json.contains("\"baselineId\": \"native_home_message\""));
        assertTrue(json.contains("\"runId\": \"100\""));
        assertTrue(json.contains("\"id\": \"native-raw-xml\""));
        assertTrue(json.contains("\"path\": \"native/raw/native_xml_100.xml\""));
        assertTrue(json.contains("\"id\": \"native-semantic-snapshot\""));
        assertTrue(json.contains("\"artifactCount\": 2"));
        assertTrue(json.contains("\"status\": \"PASS\""));
    }

    @Test
    public void marksEmptyArtifactAsFailed() throws Exception {
        File runDir = temporaryFolder.newFolder("run");
        write(new File(runDir, "native/raw/native_xml_100.xml"), "");

        File result = OnDeviceEvaluationRunner.generate(
                runDir, "native_home_message", "100", 1234L);

        String json = read(result);
        assertTrue(json.contains("\"schemaStatus\": \"FAIL\""));
        assertTrue(json.contains("\"status\": \"FAIL\""));
    }

    private static void write(File file, String content) throws Exception {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("创建目录失败: " + parent.getAbsolutePath());
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
