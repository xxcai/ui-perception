package com.hh.uiperception.webplugin;

import com.hh.uiperception.core.semantic.Bounds;
import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.core.semantic.SemanticRole;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 将 WebDomSerializer JS 输出的 JSON 解析为 SemanticNode 树。
 */
public final class WebJsonParser {

    private WebJsonParser() {}

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

    private static Bounds parseBounds(JSONArray arr) throws JSONException {
        if (arr == null || arr.length() != 4) {
            return null;
        }
        return new Bounds(arr.getInt(0), arr.getInt(1), arr.getInt(2), arr.getInt(3));
    }
}
