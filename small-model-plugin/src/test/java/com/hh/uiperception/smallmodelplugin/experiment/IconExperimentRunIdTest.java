package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class IconExperimentRunIdTest {

    @Test
    public void createsStableReadableRunId() {
        String runId = IconExperimentRunId.create(
                IconInputMode.FULL_IMAGE_WITH_BOUNDS,
                1778486400000L
        );

        assertTrue(runId.startsWith("20260511_"));
        assertTrue(runId.contains("_FULL_IMAGE_WITH_BOUNDS_"));
        assertTrue(runId.matches("\\d{8}_\\d{6}_FULL_IMAGE_WITH_BOUNDS_\\d{3}"));
    }
}
