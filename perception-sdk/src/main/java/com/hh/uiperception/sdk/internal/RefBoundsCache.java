package com.hh.uiperception.sdk.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RefBoundsCache {

    private static final Pattern REF_PATTERN = Pattern.compile("\\[ref=(n\\d+|w\\d+)]");
    private static final Pattern BOUNDS_PATTERN = Pattern.compile("\\[bounds=(\\d+),(\\d+),(\\d+),(\\d+)]");

    private static final Map<String, int[]> boundsCache = new HashMap<>();
    private static final Map<String, Integer> webElementMap = new HashMap<>();
    private static int[] webViewOffset = {0, 0};

    private RefBoundsCache() {}

    static void update(String yaml) {
        update(yaml, null, null);
    }

    static void update(String yaml, Map<String, Integer> elementMap, int[] offset) {
        boundsCache.clear();
        webElementMap.clear();
        webViewOffset = new int[]{0, 0};

        if (yaml == null) return;

        for (String line : yaml.split("\n")) {
            Matcher refMatcher = REF_PATTERN.matcher(line);
            if (!refMatcher.find()) continue;
            String ref = refMatcher.group(1);

            Matcher boundsMatcher = BOUNDS_PATTERN.matcher(line);
            if (!boundsMatcher.find()) continue;

            int[] bounds = new int[]{
                    Integer.parseInt(boundsMatcher.group(1)),
                    Integer.parseInt(boundsMatcher.group(2)),
                    Integer.parseInt(boundsMatcher.group(3)),
                    Integer.parseInt(boundsMatcher.group(4))
            };
            boundsCache.put(ref, bounds);
        }

        if (elementMap != null) {
            webElementMap.putAll(elementMap);
        }
        if (offset != null) {
            webViewOffset = offset;
        }
    }

    static int[] getBounds(String ref) {
        return boundsCache.get(ref);
    }

    static boolean isWebRef(String ref) {
        return ref != null && ref.startsWith("w");
    }

    static int[] getScreenCoords(String ref) {
        int[] bounds = boundsCache.get(ref);
        if (bounds == null) return null;

        float cx = (bounds[0] + bounds[2]) / 2f;
        float cy = (bounds[1] + bounds[3]) / 2f;

        if (isWebRef(ref)) {
            cx += webViewOffset[0];
            cy += webViewOffset[1];
        }

        return new int[]{(int) cx, (int) cy};
    }

    static int getWebElementIdx(String ref) {
        Integer idx = webElementMap.get(ref);
        return idx != null ? idx : -1;
    }

    static int[] getWebViewOffset() {
        return webViewOffset;
    }

    static void clear() {
        boundsCache.clear();
        webElementMap.clear();
        webViewOffset = new int[]{0, 0};
    }
}
