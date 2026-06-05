package com.hh.uiperception.webplugin;

import com.hh.uiperception.core.semantic.Bounds;
import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.core.semantic.SemanticRole;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses the JSON output from dom-serializer.js into a SemanticNode tree.
 *
 * Data flow: WebView → evaluateJavascript(dom-serializer.js) → JSON string → this parser → SemanticNode tree
 *
 * Expected JSON structure:
 * <pre>{@code
 * {
 *   "url": "https://...",
 *   "title": "Page Title",
 *   "root": {
 *     "role": "screen",
 *     "name": "",
 *     "states": [],
 *     "bounds": [],
 *     "__pr_idx": 0,
 *     "children": [
 *       { "role": "button", "name": "Submit", "states": ["clickable"], "__pr_idx": 1, "bounds": [10,20,100,50], "children": [] }
 *     ]
 *   }
 * }
 * }</pre>
 */
public final class WebJsonParser {

    private WebJsonParser() {}

    /** Entry point: parse the top-level JSON and extract the "root" node. */
    public static SemanticNode parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONObject rootNode = root.optJSONObject("root");
            if (rootNode == null) {
                return null;
            }
            return parseNode(rootNode);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    /**
     * Recursively parse a JSON node into a SemanticNode.
     * Maps JSON fields: role → SemanticRole, name, bounds, states, __pr_idx → webElementIdx, children → recursive.
     */
    private static SemanticNode parseNode(JSONObject obj) throws JSONException {
        String roleStr = obj.optString("role", "");
        SemanticRole role = mapRole(roleStr);
        if (role == null) {
            return null;
        }

        String name = obj.optString("name", "");
        Bounds bounds = parseBounds(obj.optJSONArray("bounds"));

        SemanticNode.Builder builder = SemanticNode.builder(role)
                .name(name)
                .bounds(bounds)
                .webElementIdx(obj.optInt("__pr_idx", -1));

        JSONArray states = obj.optJSONArray("states");
        if (states != null) {
            for (int i = 0; i < states.length(); i++) {
                builder.addState(states.getString(i));
            }
        }

        JSONArray children = obj.optJSONArray("children");
        if (children != null) {
            for (int i = 0; i < children.length(); i++) {
                JSONObject child = children.optJSONObject(i);
                if (child != null) {
                    SemanticNode childNode = parseNode(child);
                    if (childNode != null) {
                        builder.addChild(childNode);
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Map a role string from JS to SemanticRole enum.
     * Matches against SemanticRole.snapshotName(); unknown roles default to GENERIC.
     */
    private static SemanticRole mapRole(String role) {
        if (role == null || role.isEmpty()) {
            return SemanticRole.GENERIC;
        }
        for (SemanticRole sr : SemanticRole.values()) {
            if (sr.snapshotName().equals(role)) {
                return sr;
            }
        }
        return SemanticRole.GENERIC;
    }

    /** Parse a [x1, y1, x2, y2] JSON array into a Bounds object. */
    private static Bounds parseBounds(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() != 4) {
            return null;
        }
        return new Bounds(arr.getInt(0), arr.getInt(1), arr.getInt(2), arr.getInt(3));
    }
}
