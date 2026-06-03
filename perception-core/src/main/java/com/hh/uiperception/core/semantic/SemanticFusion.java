package com.hh.uiperception.core.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 native 和 web SemanticNode 树融合为统一输出。
 */
public final class SemanticFusion {

    private SemanticFusion() {}

    public static String fuse(SemanticNode nativeTree, SemanticNode webTree) {
        if (nativeTree == null && webTree == null) {
            return "";
        }
        if (nativeTree == null) {
            return renderSingle(webTree, "w");
        }
        if (webTree == null) {
            return renderSingle(nativeTree, "n");
        }

        SemanticNode marked = injectWebMarker(nativeTree);
        SemanticNode nativeWithRefs = RefAssigner.assign(marked, "n");
        SemanticNode webWithRefs = RefAssigner.assign(webTree, "w", true);

        StringBuilder sb = new StringBuilder();
        sb.append(SnapshotRenderer.render(nativeWithRefs));
        sb.append("\n\n--- Web ---\n");
        sb.append(SnapshotRenderer.render(webWithRefs));
        return sb.toString();
    }

    private static String renderSingle(SemanticNode tree, String prefix) {
        boolean webMode = "w".equals(prefix);
        SemanticNode withRefs = RefAssigner.assign(tree, prefix, webMode);
        return SnapshotRenderer.render(withRefs);
    }

    private static SemanticNode injectWebMarker(SemanticNode node) {
        if (isWebView(node)) {
            return rebuildWithState(node, "web");
        }

        List<SemanticNode> rebuilt = new ArrayList<>();
        boolean changed = false;
        for (SemanticNode child : node.children()) {
            SemanticNode rebuiltChild = injectWebMarker(child);
            rebuilt.add(rebuiltChild);
            if (rebuiltChild != child) {
                changed = true;
            }
        }

        if (!changed) {
            return node;
        }

        SemanticNode.Builder builder = SemanticNode.builder(node.role())
                .name(node.name())
                .text(node.text())
                .contentDescription(node.contentDescription())
                .resourceId(node.resourceId())
                .className(node.className())
                .bounds(node.bounds())
                .roleDecision(node.roleDecision());
        if (node.hasRef()) {
            builder.ref(node.ref());
        }
        for (String state : node.states()) {
            builder.addState(state);
        }
        for (SemanticNode child : rebuilt) {
            builder.addChild(child);
        }
        return builder.build();
    }

    private static boolean isWebView(SemanticNode node) {
        return node.role() == SemanticRole.WEBVIEW;
    }

    private static SemanticNode rebuildWithState(SemanticNode node, String state) {
        SemanticNode.Builder builder = SemanticNode.builder(node.role())
                .name(node.name())
                .text(node.text())
                .contentDescription(node.contentDescription())
                .resourceId(node.resourceId())
                .className(node.className())
                .bounds(node.bounds())
                .roleDecision(node.roleDecision());
        if (node.hasRef()) {
            builder.ref(node.ref());
        }
        for (String s : node.states()) {
            builder.addState(s);
        }
        builder.addState(state);
        for (SemanticNode child : node.children()) {
            builder.addChild(child);
        }
        return builder.build();
    }
}
