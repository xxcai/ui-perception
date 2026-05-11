package com.hh.uiperception.smallmodelplugin.api;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public final class SmallModelInitConfigTest {

    @Test
    public void resolvesModelPathFromExternalBaseDirFirst() {
        String path = SmallModelInitConfig.resolveDefaultModelPath(
                new File("/sdcard/Android/data/com.hh.uiperception/files"),
                new File("/data/user/0/com.hh.uiperception/files")
        );

        assertEquals(""
                        + "/sdcard/Android/data/com.hh.uiperception/files/"
                        + SmallModelInitConfig.DEFAULT_MODEL_RELATIVE_PATH,
                path);
    }

    @Test
    public void fallsBackToInternalBaseDirWhenExternalBaseDirMissing() {
        String path = SmallModelInitConfig.resolveDefaultModelPath(
                null,
                new File("/data/user/0/com.hh.uiperception/files")
        );

        assertEquals(""
                        + "/data/user/0/com.hh.uiperception/files/"
                        + SmallModelInitConfig.DEFAULT_MODEL_RELATIVE_PATH,
                path);
    }

    @Test
    public void usesRelativePathWhenNoContextBaseDir() {
        String path = SmallModelInitConfig.resolveDefaultModelPath(null, null);

        assertEquals(SmallModelInitConfig.DEFAULT_MODEL_RELATIVE_PATH, path);
    }
}
