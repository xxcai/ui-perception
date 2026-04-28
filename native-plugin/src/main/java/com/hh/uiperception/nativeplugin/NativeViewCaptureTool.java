package com.hh.uiperception.nativeplugin;

import android.app.Activity;

import com.hh.uiperception.core.CaptureRequest;
import com.hh.uiperception.core.CaptureResult;

/**
 * 原生 View 抓取工具：遍历 Activity View 层级，输出原始 XML。
 * 必须在 UI 线程调用。
 */
final class NativeViewCaptureTool {

    private static final String NAME = "native_xml";

    static CaptureResult capture(Activity activity, CaptureRequest request) {
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
