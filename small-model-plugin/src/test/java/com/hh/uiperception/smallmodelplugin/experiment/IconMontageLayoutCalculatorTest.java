package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class IconMontageLayoutCalculatorTest {

    @Test
    public void calculatesStableMontageLayoutForFixtureTargets() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                IconExperimentFixtureTest.readResource("icon-experiment/welink_message_001/targets.json")
        );

        IconMontageLayout layout = IconMontageLayoutCalculator.calculate(testSet.targets());

        assertEquals(660, layout.width());
        assertEquals(950, layout.height());
        assertEquals(3, layout.columns());
        assertEquals(15, layout.mappings().size());

        IconTargetMapping phone = findMapping(layout, "t002");
        assertEquals(922, phone.originalBounds().left());
        assertEquals(161, phone.originalBounds().top());
        assertTrue(phone.inputBounds().width() > 0);
        assertTrue(phone.inputBounds().height() > 0);
    }

    private static IconTargetMapping findMapping(IconMontageLayout layout, String targetId) {
        for (IconTargetMapping mapping : layout.mappings()) {
            if (targetId.equals(mapping.targetId())) {
                return mapping;
            }
        }
        throw new AssertionError("mapping not found: " + targetId);
    }
}
