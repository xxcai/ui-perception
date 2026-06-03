package com.hh.uiperception.core;

import android.app.Activity;

import com.hh.uiperception.core.semantic.SemanticNode;

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
     * 插件是否可以处理当前 Activity。
     * 默认返回 true（向后兼容）。
     */
    default boolean canHandle(Activity activity) {
        return true;
    }

    /**
     * 执行本技术方向的抓取能力；不支持时返回 null。
     */
    CaptureResult capture(Activity activity, CaptureRequest request);

    /**
     * 执行本技术方向的转换能力；不支持时返回 null。
     */
    TransformResult transform(TransformRequest request);

    /**
     * 将 capture 结果解析为 SemanticNode 树（不含 ref 赋值和渲染）。
     * 供 Composer 层融合使用。不支持时返回 null。
     */
    default SemanticNode buildSemanticTree(CaptureResult captureResult) {
        return null;
    }
}
