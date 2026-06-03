package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

/**
 * raw native XML 到 semantic snapshot 文本的组合入口。
 */
public final class NativeSemanticSnapshotGenerator {

    private NativeSemanticSnapshotGenerator() {
    }

    public static String generate(String rawXml) {
        return generate(rawXml, SnapshotRenderOptions.defaults());
    }

    public static String generate(String rawXml, SnapshotRenderOptions options) {
        NativeViewNode viewRoot = NativeViewXmlParser.parse(rawXml);
        SemanticNode semanticRoot = NativeSemanticTreeBuilder.build(viewRoot);
        SemanticNode refAssignedRoot = RefAssigner.assign(semanticRoot);
        return SnapshotRenderer.render(refAssignedRoot, options);
    }
}
