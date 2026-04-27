package com.hh.uiperception.capture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抓取通道注册表。
 * 静态注册所有可用的抓取通道，支持按名称查找。
 */
public final class CaptureChannelRegistry {

    private static final Map<String, CaptureChannel> CHANNELS = new LinkedHashMap<>();

    private CaptureChannelRegistry() {
    }

    public static synchronized void register(CaptureChannel channel) {
        if (CHANNELS.containsKey(channel.name())) {
            throw new IllegalArgumentException("重复的抓取通道: " + channel.name());
        }
        CHANNELS.put(channel.name(), channel);
    }

    public static synchronized CaptureChannel findByName(String name) {
        return CHANNELS.get(name);
    }

    public static synchronized List<CaptureChannel> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(CHANNELS.values()));
    }

    public static synchronized void clearForTest() {
        CHANNELS.clear();
    }
}
