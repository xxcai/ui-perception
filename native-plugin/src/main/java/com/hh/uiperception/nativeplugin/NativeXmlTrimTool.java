package com.hh.uiperception.nativeplugin;

import com.hh.uiperception.core.TrimRequest;
import com.hh.uiperception.core.TrimResult;

/**
 * 原生 XML 裁剪工具：只保留适合模型消费的核心 View 属性。
 */
final class NativeXmlTrimTool {

    private static final String NAME = "native_xml_basic_trim";

    static TrimResult trim(TrimRequest request) {
        String trimmedXml = XmlTrimmer.trim(request.sourceContent());
        return TrimResult.success(
                NAME,
                request.sourceToolName(),
                request.baselineId(),
                request.sourceContentType(),
                trimmedXml,
                request.activityClassName()
        );
    }
}
