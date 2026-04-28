package com.hh.uiperception.nativeplugin;

import android.app.Activity;

import com.hh.uiperception.capture.CaptureChannel;
import com.hh.uiperception.capture.CaptureRequest;
import com.hh.uiperception.capture.CaptureResult;

/**
 * 原生 XML 抓取通道：遍历 Activity View 层级，输出完整结构化 XML。
 * 必须在 UI 线程调用。
 */
public final class NativeXmlChannel implements CaptureChannel {

    private static final String NAME = "native_xml";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "遍历原生 View 层级，输出包含完整属性的 XML。";
    }

    @Override
    public CaptureResult capture(Activity activity, CaptureRequest request) {
        ViewHierarchyDumper.DumpResult result = ViewHierarchyDumper.dump(activity);
        if (!result.success) {
            return CaptureResult.error(NAME, request.baselineId(), result.errorMessage);
        }
        return CaptureResult.success(
                NAME, request.baselineId(),
                "text/xml", result.xml,
                result.activityClassName
        );
    }
}
