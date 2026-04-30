package com.hh.uiperception.nativeplugin;

import com.hh.uiperception.core.TransformRequest;
import com.hh.uiperception.core.TransformResult;

/**
 * 原生 XML 转换工具：将原始 View 层级处理成适合模型消费的 XML。
 */
final class NativeXmlTransformTool {

    private static final String NAME = "native_xml_basic_transform";

    static TransformResult transform(TransformRequest request) {
        String transformedXml = XmlTrimmer.trim(request.sourceContent());
        return TransformResult.success(
                NAME,
                request.sourceToolName(),
                request.baselineId(),
                request.sourceContentType(),
                transformedXml,
                request.activityClassName()
        );
    }
}
