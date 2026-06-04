package com.hh.uiperception.core.semantic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FusionResult {

    private final String yaml;
    private final Map<String, Integer> webElementMap;

    FusionResult(String yaml, Map<String, Integer> webElementMap) {
        this.yaml = yaml;
        this.webElementMap = webElementMap != null ? webElementMap : Collections.emptyMap();
    }

    public String yaml() {
        return yaml;
    }

    public Map<String, Integer> webElementMap() {
        return webElementMap;
    }
}
