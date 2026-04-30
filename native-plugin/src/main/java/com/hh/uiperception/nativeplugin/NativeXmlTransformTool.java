package com.hh.uiperception.nativeplugin;

import com.hh.uiperception.core.TransformRequest;
import com.hh.uiperception.core.TransformResult;
import com.hh.uiperception.nativeplugin.semantic.NativeSemanticSnapshotGenerator;

/**
 * 原生 XML 转换工具：将原始 View 层级处理成适合模型消费的语义快照。
 */
final class NativeXmlTransformTool {

    private static final String NAME = "native_semantic_snapshot";
    private static final String CONTENT_TYPE = "text/yaml";

    static TransformResult transform(TransformRequest request) {
        String snapshot = NativeSemanticSnapshotGenerator.generate(request.sourceContent());
        return TransformResult.success(
                NAME,
                request.sourceToolName(),
                request.baselineId(),
                CONTENT_TYPE,
                snapshot,
                request.activityClassName()
        );
    }
}
