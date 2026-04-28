package com.hh.uiperception.capture.nativexml;

import com.hh.uiperception.capture.CaptureChannelRegistry;
import com.hh.uiperception.capture.CapturePlugin;

/**
 * 原生 XML 抓取方向插件。
 * 注册 native_xml 通道到 CaptureChannelRegistry。
 */
public final class NativeXmlPlugin implements CapturePlugin {

    @Override
    public void register() {
        CaptureChannelRegistry.register(new NativeXmlChannel());
    }
}
