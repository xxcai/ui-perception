package com.hh.uiperception.baseline;

import java.util.Objects;

public final class BaselineSpec {
    private final String id;
    private final String title;
    private final String description;
    private final BaselineType type;
    private final String route;
    private final String intentConfigAssetPath;

    public BaselineSpec(
            String id,
            String title,
            String description,
            BaselineType type,
            String route,
            String intentConfigAssetPath
    ) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.description = description == null ? "" : description;
        this.type = Objects.requireNonNull(type, "type");
        this.route = requireText(route, "route");
        this.intentConfigAssetPath = intentConfigAssetPath == null ? "" : intentConfigAssetPath;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public BaselineType type() {
        return type;
    }

    public String route() {
        return route;
    }

    public String intentConfigAssetPath() {
        return intentConfigAssetPath;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
