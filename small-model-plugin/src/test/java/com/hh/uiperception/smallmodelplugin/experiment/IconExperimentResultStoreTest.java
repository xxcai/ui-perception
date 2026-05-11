package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.Test;

public final class IconExperimentResultStoreTest {

    @Test
    public void savesRunResultJson() throws Exception {
        File runsDir = Files.createTempDirectory("icon-experiment-runs").toFile();
        IconTarget target = new IconTarget(
                "icon_phone",
                new IconBounds(922, 161, 1034, 273),
                "电话图标",
                Arrays.asList("电话")
        );
        IconExperimentRunResult result = IconExperimentRunResult.builder()
                .setRunId("20260511_160000_FULL_IMAGE_WITH_BOUNDS_001")
                .setCreatedAtMs(1778486400000L)
                .setTestsetId("welink_message_001")
                .setImage("screenshot.jpg")
                .setInputMode(IconInputMode.FULL_IMAGE_WITH_BOUNDS)
                .setTargetCount(1)
                .setImagePrepareMs(12L)
                .setModelLoadMs(300L)
                .setInferenceMs(1200L)
                .setTotalMs(1218L)
                .setPrompt("prompt")
                .setTargets(Arrays.asList(target))
                .setRawOutput("icon_phone:电话图标")
                .setParsedOutput(Arrays.asList(new ParsedIconDescription("icon_phone", "电话图标")))
                .setManualScores(Arrays.asList(new IconManualScore("icon_phone", null, "")))
                .build();

        File output = IconExperimentResultStore.save(runsDir, result);
        String json = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        IconExperimentRunResult parsed = IconExperimentJson.parseRunResult(json);

        assertTrue(output.getName().endsWith(".json"));
        assertEquals("20260511_160000_FULL_IMAGE_WITH_BOUNDS_001", parsed.runId());
        assertEquals("welink_message_001", parsed.testsetId());
        assertEquals(IconInputMode.FULL_IMAGE_WITH_BOUNDS, parsed.inputMode());
        assertEquals(1200L, parsed.inferenceMs());
        assertEquals("电话图标", parsed.parsedOutput().get(0).desc());
    }
}
