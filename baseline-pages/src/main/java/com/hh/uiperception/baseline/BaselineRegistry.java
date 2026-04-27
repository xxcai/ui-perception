package com.hh.uiperception.baseline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BaselineRegistry {
    private static final Map<String, BaselineSpec> SPECS = new LinkedHashMap<>();

    static {
        register(new BaselineSpec(
                "native_home_message",
                "原生首页-消息",
                "复刻应用首页的消息 Tab，用于测试标题区、搜索区、筛选 Tab、提示 Banner、会话列表和底部导航识别。",
                BaselineType.NATIVE,
                BaselineRoutes.NATIVE_HOME_MESSAGE,
                "baseline_intents/native_home_message.json"
        ));
        register(new BaselineSpec(
                "web_home_placeholder",
                "Web 首页占位",
                "Phase1 基准 Web 页面入口占位，当前不实现 H5 页面内容。",
                BaselineType.WEB,
                BaselineRoutes.WEB_HOME_PLACEHOLDER,
                ""
        ));
    }

    private BaselineRegistry() {
    }

    public static synchronized void register(BaselineSpec spec) {
        if (SPECS.containsKey(spec.id())) {
            throw new IllegalArgumentException("Duplicate baseline spec id: " + spec.id());
        }
        SPECS.put(spec.id(), spec);
    }

    public static synchronized List<BaselineSpec> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(SPECS.values()));
    }

    public static synchronized BaselineSpec findById(String id) {
        return SPECS.get(id);
    }

    public static synchronized void clearForTest() {
        SPECS.clear();
    }
}
