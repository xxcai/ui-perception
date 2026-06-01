package com.hh.uiperception.smallmodelplugin.experiment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实验运行 id 生成器。
 */
public final class IconExperimentRunId {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    private IconExperimentRunId() {
    }

    public static String create(IconInputMode inputMode, long createdAtMs) {
        IconInputMode mode = inputMode == null ? IconInputMode.FULL_IMAGE : inputMode;
        int sequence = SEQUENCE.updateAndGet(value -> value >= 999 ? 1 : value + 1);
        return FORMAT.format(new Date(createdAtMs))
                + "_"
                + mode.name()
                + "_"
                + String.format(Locale.US, "%03d", sequence);
    }
}
