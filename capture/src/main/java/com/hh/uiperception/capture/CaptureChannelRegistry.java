package com.hh.uiperception.capture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抓取通道注册表。
 * 初始为空，由各 CapturePlugin 在启动时注册通道。
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
