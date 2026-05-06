package com.hh.uiperception.nativeplugin.evaluation;

import com.hh.uiperception.nativeplugin.semantic.NativeSemanticRole;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 加载并校验 targets.yml。
 */
public final class EvaluationTargets {

    private static final String KEY_PAGE = "page";
    private static final String KEY_TARGETS = "targets";

    private static final String FIELD_ID = "id";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_REQUIRED_REF = "requiredRef";
    private static final String FIELD_BOUNDS = "bounds";

    private static final Set<String> ALLOWED_ROLES = allowedRoles();

    private final String page;
    private final List<EvaluationTarget> targets;

    public EvaluationTargets(String page, List<EvaluationTarget> targets) {
        this.page = page;
        this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
    }

    public String page() {
        return page;
    }

    public List<EvaluationTarget> targets() {
        return targets;
    }

    public static EvaluationTargets load(InputStream in) {
        return load(in, null);
    }

    public static EvaluationTargets load(InputStream in, String pageName) {
        return loadFromString(readFully(in), pageName);
    }

    public static EvaluationTargets loadFromString(String yamlContent) {
        return loadFromString(yamlContent, null);
    }

    static EvaluationTargets loadFromString(String yamlContent, String pageName) {
        byte[] bytes = yamlContent.getBytes(StandardCharsets.UTF_8);
        Map<String, String> scalars = SimpleYamlParser.parseScalars(
                new ByteArrayInputStream(bytes));
        List<Map<String, String>> entries = SimpleYamlParser.parseSequence(
                new ByteArrayInputStream(bytes), KEY_TARGETS);
        return loadFromParsed(scalars, entries, pageName);
    }

    static EvaluationTargets loadFromParsed(
            Map<String, String> scalars,
            List<Map<String, String>> targetEntries,
            String pageName) {
        String page = requiredPage(scalars, pageName);
        List<EvaluationTarget> targets = new ArrayList<>();
        for (int i = 0; i < targetEntries.size(); i++) {
            Map<String, String> entry = targetEntries.get(i);
            String id = required(entry, FIELD_ID, page, i);
            String role = required(entry, FIELD_ROLE, page, i);
            if (!ALLOWED_ROLES.contains(role)) {
                throw new IllegalArgumentException(
                        "targets 中 target '" + id + "' 的 role 非法: " + role
                                + " (页面: " + page + ")");
            }
            String requiredRefValue = required(entry, FIELD_REQUIRED_REF, page, i);
            boolean requiredRef = parseBoolean(requiredRefValue, id, page);
            String name = optional(entry, FIELD_NAME);
            String bounds = optional(entry, FIELD_BOUNDS);
            targets.add(new EvaluationTarget(id, role, name, requiredRef, bounds));
        }

        if (targets.isEmpty()) {
            throw new IllegalArgumentException("targets 中没有定义任何 target (页面: " + page + ")");
        }
        return new EvaluationTargets(page, targets);
    }

    private static String requiredPage(Map<String, String> scalars, String pageName) {
        String page = scalars.get(KEY_PAGE);
        if (page == null || page.isEmpty()) {
            throw new IllegalArgumentException(
                    "targets 缺少必填字段 'page'"
                            + (pageName != null ? " (页面: " + pageName + ")" : ""));
        }
        return page;
    }

    private static String required(Map<String, String> entry, String field, String page, int index) {
        String value = entry.get(field);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(
                    "targets 中第 " + (index + 1) + " 个 target 缺少 '" + field + "'"
                            + " (页面: " + page + ")");
        }
        return value;
    }

    private static String optional(Map<String, String> entry, String field) {
        String value = entry.get(field);
        return value == null ? "" : value;
    }

    private static boolean parseBoolean(String value, String id, String page) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException(
                "targets 中 target '" + id + "' 的 requiredRef 必须是 true 或 false"
                        + " (页面: " + page + ")");
    }

    private static Set<String> allowedRoles() {
        Set<String> roles = new HashSet<>();
        for (NativeSemanticRole role : NativeSemanticRole.values()) {
            roles.add(role.snapshotName());
        }
        return roles;
    }

    private static String readFully(InputStream in) {
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取 targets 失败", e);
        }
    }
}
