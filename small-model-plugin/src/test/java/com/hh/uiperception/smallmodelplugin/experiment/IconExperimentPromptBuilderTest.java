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
        java.util.List<IconTargetMapping> mappings = IconExperimentInputBuilder.buildMappings(testSet);

        String prompt = IconExperimentPromptBuilder.fullImagePrompt(mappings);

        assertTrue(prompt.contains("1"));
        assertTrue(prompt.contains("2"));
        assertFalse(prompt.contains("t002"));
        assertTrue(prompt.contains("Write every description in Simplified Chinese"));
        assertTrue(prompt.contains("Use a very short label of 1 to 4 Chinese characters only"));
        assertTrue(prompt.contains("<id>:<short Chinese label>"));
        assertFalse(prompt.contains("922,161,1034,273"));
        assertFalse(prompt.contains("电话图标"));
    }

    @Test
    public void buildsFullImageWithBoundsPrompt() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                IconExperimentFixtureTest.readResource("icon-experiment/welink_message_001/targets.json")
        );
        java.util.List<IconTargetMapping> mappings = IconExperimentInputBuilder.buildMappings(testSet);

        String prompt = IconExperimentPromptBuilder.fullImageWithBoundsPrompt(mappings);

        assertTrue(prompt.contains("2=922,161,1034,273"));
        assertTrue(prompt.contains("3=1062,161,1174,273"));
        assertFalse(prompt.contains("t002=922,161,1034,273"));
        assertTrue(prompt.contains("Coordinates are pixel coordinates in the original screenshot"));
        assertTrue(prompt.contains("Valid x range is 0 to 1216. Valid y range is 0 to 2490."));
        assertTrue(prompt.contains("The origin (0,0) is the top-left corner"));
        assertTrue(prompt.contains("x increases from left to right. y increases from top to bottom"));
        assertTrue(prompt.contains("Process the target regions one by one in the exact order listed below"));
        assertTrue(prompt.contains("Write every description in Simplified Chinese"));
        assertTrue(prompt.contains("Do not write sentences, explanations, modifiers"));
        assertTrue(prompt.contains("<id>:<short Chinese label>"));
        assertFalse(prompt.contains("电话图标"));
    }

    @Test
    public void buildsFullImageWithExplicitImageSizePrompt() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                IconExperimentFixtureTest.readResource("icon-experiment/welink_message_001/targets.json")
        );
        java.util.List<IconTargetMapping> mappings = IconExperimentInputBuilder.buildMappings(testSet);

        String prompt = IconExperimentPromptBuilder.fullImageWithBoundsPrompt(mappings, 1216, 2640);

        assertTrue(prompt.contains("The screenshot size is 1216 pixels wide and 2640 pixels high."));
        assertTrue(prompt.contains("Valid x range is 0 to 1216. Valid y range is 0 to 2640."));
    }

    @Test
    public void buildsMarkedBoundsPromptWithoutCoordinates() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                IconExperimentFixtureTest.readResource("icon-experiment/welink_message_001/targets.json")
        );
        java.util.List<IconTargetMapping> mappings = IconExperimentInputBuilder.buildMappings(testSet);

        String prompt = IconExperimentPromptBuilder.markedBoundsPrompt(mappings);

        assertTrue(prompt.contains("visible blue rectangles and id labels"));
        assertTrue(prompt.contains("Use the visible rectangle and label in the image"));
        assertTrue(prompt.contains("Write every description in Simplified Chinese"));
        assertTrue(prompt.contains("Use a very short label of 1 to 4 Chinese characters only"));
        assertTrue(prompt.contains("12"));
        assertFalse(prompt.contains("t012"));
        assertTrue(prompt.contains("Return exactly 15 lines"));
        assertFalse(prompt.contains("322,2406,406,2490"));
    }
}
