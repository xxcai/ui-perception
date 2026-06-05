package com.hh.uiperception.webplugin;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Loads and caches the DOM serializer JavaScript from assets.
 *
 * The script is injected into WebView via evaluateJavascript to serialize the
 * visible DOM into a JSON tree of semantic nodes. See dom-serializer.js for
 * the full implementation with Playwright ARIA snapshot pipeline references.
 */
public final class WebDomSerializer {

    private static volatile String cachedScript = null;

    private WebDomSerializer() {}

    /**
     * Load the DOM serializer JS from assets (cached after first load).
     *
     * @param context Android context for accessing assets
     * @return the complete JS script as a string
     */
    public static String script(Context context) {
        if (cachedScript != null) return cachedScript;
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            context.getAssets().open("dom-serializer.js"), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            cachedScript = sb.toString();
            return cachedScript;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load dom-serializer.js from assets", e);
        }
    }
}
