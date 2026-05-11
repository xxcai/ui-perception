package com.hh.uiperception.smallmodelplugin.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public final class IconExperimentFixtureTest {

    @Test
    public void loadsWelinkMessageFixture() throws Exception {
        IconExperimentTestSet testSet = IconExperimentJson.parseTestSet(
                readResource("icon-experiment/welink_message_001/targets.json")
        );

        assertEquals("welink_message_001", testSet.testsetId());
        assertEquals("screenshot.jpg", testSet.image());
        assertEquals(15, testSet.targets().size());
        assertTargetBounds(testSet, "icon_phone", 922, 161, 1034, 273);
        assertTargetBounds(testSet, "icon_add", 1062, 161, 1174, 273);
        assertTargetBounds(testSet, "icon_tab_mail", 322, 2406, 406, 2490);
        assertNotNull(resourceStream("icon-experiment/welink_message_001/screenshot.jpg"));
        assertNotNull(resourceStream("icon-experiment/welink_message_001/window.xml"));
    }

    private static void assertTargetBounds(
            IconExperimentTestSet testSet,
            String id,
            int left,
            int top,
            int right,
            int bottom
    ) {
        IconTarget target = findTarget(testSet, id);
        assertEquals(left, target.bounds().left());
        assertEquals(top, target.bounds().top());
        assertEquals(right, target.bounds().right());
        assertEquals(bottom, target.bounds().bottom());
    }

    private static IconTarget findTarget(IconExperimentTestSet testSet, String id) {
        for (IconTarget target : testSet.targets()) {
            if (id.equals(target.id())) {
                return target;
            }
        }
        throw new AssertionError("target not found: " + id);
    }

    static String readResource(String path) throws Exception {
        try (InputStream inputStream = resourceStream(path)) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static InputStream resourceStream(String path) {
        InputStream inputStream = IconExperimentFixtureTest.class
                .getClassLoader()
                .getResourceAsStream(path);
        if (inputStream == null) {
            throw new AssertionError("resource not found: " + path);
        }
        return inputStream;
    }
}
