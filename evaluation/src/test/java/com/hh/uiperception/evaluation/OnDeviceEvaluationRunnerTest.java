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

    @Test
    public void writesCountResultsForMinimumTextCount() throws Exception {
        File runDir = temporaryFolder.newFolder("run");
        write(new File(runDir, "native/transformed/native_semantic_snapshot_101.yml"),
                ""
                        + "- text \"邮件\"\n"
                        + "- button [ref=n1]:\n"
                        + "  - text \"邮件\"\n");

        File result = OnDeviceEvaluationRunner.generate(
                runDir, "native_home_mail", "100", 1234L,
                ""
                        + "page: native_home_mail\n"
                        + "targets:\n"
                        + "  - id: mail-label-count\n"
                        + "    role: text\n"
                        + "    name: 邮件\n"
                        + "    minCount: 2\n");

        String json = read(result);
        assertTrue(json.contains("\"countResults\""));
        assertTrue(json.contains("\"id\": \"mail-label-count\""));
        assertTrue(json.contains("\"minCount\": 2"));
        assertTrue(json.contains("\"actualCount\": 2"));
        assertTrue(json.contains("\"countTargetPassCount\": 1"));
        assertTrue(json.contains("\"status\": \"PASS\""));
    }

    @Test
    public void writesTargetResultsForInformationEvidence() throws Exception {
        File runDir = temporaryFolder.newFolder("run");
        write(new File(runDir, "native/transformed/native_semantic_snapshot_101.yml"),
                ""
                        + "- text \"消息\"\n"
                        + "- scroll [ref=n1]\n"
                        + "- button \"搜索\" [ref=n2]\n"
                        + "- button [ref=n3]:\n"
                        + "  - text \"消息\"\n");

        File result = OnDeviceEvaluationRunner.generate(
                runDir, "native_home_message", "100", 1234L,
                ""
                        + "page: native_home_message\n"
                        + "targets:\n"
                        + "  - id: page-identity\n"
                        + "    type: information\n"
                        + "    description: LLM 能否判断当前页面是消息页\n"
                        + "    evidence:\n"
                        + "      - id: message-title-and-tab\n"
                        + "        role: text\n"
                        + "        name: 消息\n"
                        + "        minCount: 2\n"
                        + "  - id: search-entry\n"
                        + "    type: information\n"
                        + "    description: LLM 能否知道消息页有搜索入口\n"
                        + "    evidence:\n"
                        + "      - id: search-button\n"
                        + "        role: button\n"
                        + "        name: 搜索\n"
                        + "        minCount: 1\n");

        String json = read(result);
        assertTrue(json.contains("\"targetResults\""));
        assertTrue(json.contains("\"id\": \"page-identity\""));
        assertTrue(json.contains("\"description\": \"LLM 能否判断当前页面是消息页\""));
        assertTrue(json.contains("\"id\": \"message-title-and-tab\""));
        assertTrue(json.contains("\"actualCount\": 2"));
        assertTrue(json.contains("\"score\": 1.00"));
        assertTrue(json.contains("\"targetCount\": 2"));
        assertTrue(json.contains("\"targetPassCount\": 2"));
        assertTrue(json.contains("\"evidenceCount\": 2"));
        assertTrue(json.contains("\"evidencePassCount\": 2"));
        assertTrue(json.contains("\"status\": \"PASS\""));
    }

    @Test
    public void defaultsMinimumCountToOne() throws Exception {
        File runDir = temporaryFolder.newFolder("run");
        write(new File(runDir, "native/transformed/native_semantic_snapshot_101.yml"),
                ""
                        + "- button \"搜索\" [ref=n1]\n"
                        + "- text \"客服\"\n");

        File result = OnDeviceEvaluationRunner.generate(
                runDir, "native_home_message", "100", 1234L,
                ""
                        + "page: native_home_message\n"
                        + "targets:\n"
                        + "  - id: search-count\n"
                        + "    role: button\n"
                        + "    name: 搜索\n"
                        + "  - id: service-entry\n"
                        + "    type: information\n"
                        + "    description: LLM 能否知道有客服入口\n"
                        + "    evidence:\n"
                        + "      - id: service-text\n"
                        + "        role: text\n"
                        + "        name: 客服\n");

        String json = read(result);
        assertTrue(json.contains("\"id\": \"search-count\""));
        assertTrue(json.contains("\"id\": \"service-text\""));
        assertTrue(json.contains("\"minCount\": 1"));
        assertTrue(json.contains("\"targetPassCount\": 1"));
        assertTrue(json.contains("\"status\": \"PASS\""));
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
