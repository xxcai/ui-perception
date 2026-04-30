package com.hh.uiperception.nativeplugin.semantic;

/**
 * raw native XML 到 semantic snapshot 文本的组合入口。
 */
public final class NativeSemanticSnapshotGenerator {

    private NativeSemanticSnapshotGenerator() {
    }

    public static String generate(String rawXml) {
        return generate(rawXml, NativeSnapshotRenderOptions.defaults());
    }

    public static String generate(String rawXml, NativeSnapshotRenderOptions options) {
        NativeViewNode viewRoot = NativeViewXmlParser.parse(rawXml);
        NativeSemanticNode semanticRoot = NativeSemanticTreeBuilder.build(viewRoot);
        NativeSemanticNode refAssignedRoot = NativeRefAssigner.assign(semanticRoot);
        return NativeSnapshotRenderer.render(refAssignedRoot, options);
    }
}
