package com.hh.uiperception.core;

import android.app.Activity;

/**
 * 感知插件接口：每个技术方向直接暴露本方向支持的阶段能力。
 * 不支持的阶段返回 null。
 */
public interface PerceptionPlugin {

    /**
     * 插件唯一标识，如 "native"、"web"、"ocr"。
     */
    String name();

    /**
     * 执行本技术方向的抓取能力；不支持时返回 null。
     */
    CaptureResult capture(Activity activity, CaptureRequest request);

    /**
     * 执行本技术方向的裁剪能力；不支持时返回 null。
     */
    TrimResult trim(TrimRequest request);
}
