package com.hh.uiperception.nativeplugin;

import android.app.Activity;

import com.hh.uiperception.core.CaptureRequest;
import com.hh.uiperception.core.CaptureResult;
import com.hh.uiperception.core.PerceptionPlugin;
import com.hh.uiperception.core.TransformRequest;
import com.hh.uiperception.core.TransformResult;

/**
 * Native 技术方向工具集。
 * 直接暴露 native 方向支持的抓取和转换能力。
 */
public final class NativePerceptionPlugin implements PerceptionPlugin {

    @Override
    public String name() {
        return "native";
    }

    @Override
    public CaptureResult capture(Activity activity, CaptureRequest request) {
        return NativeViewCaptureTool.capture(activity, request);
    }

    @Override
    public TransformResult transform(TransformRequest request) {
        return NativeXmlTransformTool.transform(request);
    }
}
