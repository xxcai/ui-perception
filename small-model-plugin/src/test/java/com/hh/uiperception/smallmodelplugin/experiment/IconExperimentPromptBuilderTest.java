package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class IconExperimentPromptBuilderTest {

    @Test
    public void buildsFullImageBaselinePromptWithoutBounds() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                IconExperimentFixtureTest.readResource("icon-experiment/welink_message_001/targets.json")
        );

        String prompt = IconExperimentPromptBuilder.fullImagePrompt(testSet);

        assertTrue(prompt.contains("icon_phone"));
        assertTrue(prompt.contains("icon_add"));
        assertTrue(prompt.contains("<id>:<short Chinese description>"));
        assertFalse(prompt.contains("922,161,1034,273"));
    }

    @Test
    public void buildsFullImageWithBoundsPrompt() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                IconExperimentFixtureTest.readResource("icon-experiment/welink_message_001/targets.json")
        );

        String prompt = IconExperimentPromptBuilder.fullImageWithBoundsPrompt(testSet);

        assertTrue(prompt.contains("icon_phone=922,161,1034,273"));
        assertTrue(prompt.contains("icon_add=1062,161,1174,273"));
        assertTrue(prompt.contains("Inspect only pixels inside the region bounds"));
        assertTrue(prompt.contains("<id>:<short Chinese description>"));
    }
}
