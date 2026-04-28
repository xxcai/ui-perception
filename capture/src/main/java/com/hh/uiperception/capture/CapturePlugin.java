package com.hh.uiperception.capture;

/**
 * 抓取插件接口：每个抓取方向模块实现此接口，在 app 启动时注册本模块的通道。
 *
 * 例：capture-native-xml 提供 NativeXmlPlugin，
 *     capture-screenshot 提供 ScreenshotPlugin（同时注册 OCR 和视觉模型通道）。
 */
public interface CapturePlugin {

    /**
     * 注册本模块的所有抓取通道到 CaptureChannelRegistry。
     */
    void register();
}
