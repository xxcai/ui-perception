package com.hh.uiperception.core;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 插件注册中心。
 * 宿主通过 register() 注册插件，调用方通过 getApplicable() 获取适用的插件列表。
 */
public final class PluginRegistry {

    private static final CopyOnWriteArrayList<PerceptionPlugin> plugins = new CopyOnWriteArrayList<>();

    private PluginRegistry() {}

    /** 注册一个插件。通常在 SDK 初始化时调用。 */
    public static void register(PerceptionPlugin plugin) {
        if (plugin != null && !plugins.contains(plugin)) {
            plugins.add(plugin);
        }
    }

    /** 返回所有 canHandle(activity) == true 的插件，按注册顺序。 */
    public static List<PerceptionPlugin> getApplicable(Activity activity) {
        if (activity == null) {
            return Collections.emptyList();
        }
        List<PerceptionPlugin> result = new ArrayList<>();
        for (PerceptionPlugin plugin : plugins) {
            if (plugin.canHandle(activity)) {
                result.add(plugin);
            }
        }
        return result;
    }

    /** 返回所有已注册插件（不过滤）。 */
    public static List<PerceptionPlugin> all() {
        return Collections.unmodifiableList(new ArrayList<>(plugins));
    }

    /** 清除所有已注册插件。 */
    public static void clear() {
        plugins.clear();
    }
}
