package com.hh.uiperception.webplugin;

import com.hh.uiperception.core.semantic.SemanticNode;
import com.hh.uiperception.core.semantic.SemanticRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Web DOM 树归一化：generic 折叠 + 冗余文本去除。
 */
public final class TreeNormalizer {

    private TreeNormalizer() {}

    public static SemanticNode normalize(SemanticNode root) {
        if (root == null) {
            return null;
        }
        return normalizeNode(root);
    }

    private static SemanticNode normalizeNode(SemanticNode node) {
        List<SemanticNode> normalized = new ArrayList<>();
        for (SemanticNode child : node.children()) {
            SemanticNode n = normalizeNode(child);
            if (n != null) {
                normalized.add(n);
            }
        }

        // Generic 折叠：无名称、无 states、无 ref、只有一个子节点 → 用子节点替代
        if (node.role() == SemanticRole.GENERIC
                && node.name().isEmpty()
                && node.states().isEmpty()
                && !node.hasRef()
                && normalized.size() == 1) {
            return normalized.get(0);
        }

        // 冗余文本去除：唯一子节点是 text 且内容等于 name → 移除
        if (normalized.size() == 1
                && normalized.get(0).role() == SemanticRole.TEXT
                && !node.name().isEmpty()
                && node.name().equals(normalized.get(0).name())) {
            normalized.clear();
        }

        if (sameChildren(node.children(), normalized)) {
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
        for (SemanticNode child : normalized) {
            builder.addChild(child);
        }
        return builder.build();
    }

    private static boolean sameChildren(List<SemanticNode> a, List<SemanticNode> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }
}
