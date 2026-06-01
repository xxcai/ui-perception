package com.hh.uiperception.smallmodelplugin.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一张截图及其待识别图标目标配置。
 */
public final class IconExperimentTestSet {

    private final String testsetId;
    private final String image;
    private final List<IconTarget> targets;

    public IconExperimentTestSet(String testsetId, String image, List<IconTarget> targets) {
        this.testsetId = normalize(testsetId);
        this.image = normalize(image);
        this.targets = Collections.unmodifiableList(new ArrayList<>(
                targets == null ? Collections.emptyList() : targets
        ));
    }

    public String testsetId() {
        return testsetId;
    }

    public String image() {
        return image;
    }

    public List<IconTarget> targets() {
        return targets;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
