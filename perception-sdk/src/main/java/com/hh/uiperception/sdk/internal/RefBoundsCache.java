package com.hh.uiperception.sdk.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RefBoundsCache {

    private static final Pattern REF_PATTERN = Pattern.compile("\\[ref=(n\\d+)]");
    private static final Pattern BOUNDS_PATTERN = Pattern.compile("\\[bounds=(\\d+),(\\d+),(\\d+),(\\d+)]");

    private static final Map<String, int[]> cache = new HashMap<>();

    private RefBoundsCache() {}

    static void update(String yaml) {
        cache.clear();
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
            cache.put(ref, bounds);
        }
    }

    static int[] getBounds(String ref) {
        return cache.get(ref);
    }

    static void clear() {
        cache.clear();
    }
}
