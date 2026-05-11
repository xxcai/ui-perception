package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

public final class IconExperimentJsonTest {

    @Test
    public void parsesTestSetJson() throws Exception {
        String json = "{"
                + "\"testset_id\":\"message_001\","
                + "\"image\":\"screenshot.png\","
                + "\"targets\":[{"
                + "\"id\":\"icon_001\","
                + "\"bounds\":[828,168,933,273],"
                + "\"expected\":\"电话图标\","
                + "\"acceptable\":[\"电话\",\"拨号\"]"
                + "}]"
                + "}";

        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(json);

        assertEquals("message_001", testSet.testsetId());
        assertEquals("screenshot.png", testSet.image());
        assertEquals(1, testSet.targets().size());
        IconTarget target = testSet.targets().get(0);
        assertEquals("icon_001", target.id());
        assertEquals(828, target.bounds().left());
        assertEquals(168, target.bounds().top());
        assertEquals(933, target.bounds().right());
        assertEquals(273, target.bounds().bottom());
        assertEquals("电话图标", target.expected());
        assertEquals(Arrays.asList("电话", "拨号"), target.acceptable());
    }

    @Test
    public void serializesAndParsesRunResult() throws Exception {
        IconTarget target = new IconTarget(
                "icon_001",
                new IconBounds(828, 168, 933, 273),
                "电话图标",
                Arrays.asList("电话", "拨号")
        );
        IconExperimentRunResult result = IconExperimentRunResult.builder()
                .setRunId("20260511_160000_001")
                .setCreatedAtMs(1778486400000L)
                .setTestsetId("message_001")
                .setImage("screenshot.png")
                .setInputMode(IconInputMode.FULL_IMAGE_WITH_BOUNDS)
                .setTargetCount(1)
                .setImagePrepareMs(8L)
                .setModelLoadMs(0L)
                .setInferenceMs(3120L)
                .setTotalMs(3140L)
                .setPrompt("prompt")
                .setTargets(Arrays.asList(target))
                .setRawOutput("icon_001:电话图标")
                .setParsedOutput(Arrays.asList(new ParsedIconDescription("icon_001", "电话图标")))
                .setManualScores(Arrays.asList(new IconManualScore("icon_001", null, "")))
                .build();

        String json = IconExperimentJson.toJson(result);
        IconExperimentRunResult parsed = IconExperimentJson.parseRunResult(json);

        assertEquals("20260511_160000_001", parsed.runId());
        assertEquals(1778486400000L, parsed.createdAtMs());
        assertEquals("message_001", parsed.testsetId());
        assertEquals(IconInputMode.FULL_IMAGE_WITH_BOUNDS, parsed.inputMode());
        assertEquals(1, parsed.targetCount());
        assertEquals(8L, parsed.imagePrepareMs());
        assertEquals(3120L, parsed.inferenceMs());
        assertEquals("icon_001:电话图标", parsed.rawOutput());
        assertEquals("电话图标", parsed.parsedOutput().get(0).desc());
        assertNull(parsed.manualScores().get(0).score());
        assertNull(parsed.error());
    }

    @Test
    public void serializesFailureRunResult() throws Exception {
        IconExperimentRunResult result = IconExperimentRunResult.builder()
                .setRunId("run_failed")
                .setInputMode(IconInputMode.CROPPED_MONTAGE)
                .setError(new IconExperimentError("MODEL_FILE_MISSING", "模型文件不存在"))
                .build();

        IconExperimentRunResult parsed = IconExperimentJson.parseRunResult(
                IconExperimentJson.toJson(result)
        );

        assertEquals("run_failed", parsed.runId());
        assertEquals(IconInputMode.CROPPED_MONTAGE, parsed.inputMode());
        assertEquals("MODEL_FILE_MISSING", parsed.error().code());
        assertEquals("模型文件不存在", parsed.error().message());
    }
}
