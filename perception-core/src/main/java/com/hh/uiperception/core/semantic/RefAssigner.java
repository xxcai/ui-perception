package com.hh.uiperception.core.semantic;

/**
 * 为 semantic tree 中可执行节点分配当前 snapshot 内 ref。
 */
public final class RefAssigner {

    private RefAssigner() {
    }

    public static SemanticNode assign(SemanticNode root) {
        return assign(root, "n", false);
    }

    public static SemanticNode assign(SemanticNode root, String prefix) {
        return assign(root, prefix, false);
    }

    public static SemanticNode assign(SemanticNode root, String prefix, boolean webMode) {
        if (root == null) {
            return null;
        }
        return new State(prefix, webMode).assignNode(root);
    }

    private static boolean shouldAssignRef(SemanticNode node, boolean webMode) {
        if (node.bounds() == null || !node.bounds().isValid()) {
            return false;
        }
        if (webMode) {
            return true;
        }
        if (node.role() == SemanticRole.LIST_ITEM) {
            return shouldAssignRefToListItem(node);
        }
        return isExecutableRole(node.role());
    }

    private static boolean isExecutableRole(SemanticRole role) {
        return role == SemanticRole.BUTTON
                || role == SemanticRole.INPUT
                || role == SemanticRole.CHECKBOX
                || role == SemanticRole.RADIO
                || role == SemanticRole.SWITCH
                || role == SemanticRole.SLIDER
                || role == SemanticRole.PICKER
                || role == SemanticRole.LIST
                || role == SemanticRole.GRID
                || role == SemanticRole.SCROLL
                || role == SemanticRole.WEBVIEW;
    }

    private static boolean shouldAssignRefToListItem(SemanticNode node) {
        if (node.states().contains(SemanticStates.CLICKABLE)
                || node.states().contains(SemanticStates.CLICKABLE_INFERRED)) {
            return true;
        }
        if (node.states().contains(SemanticStates.CLICKABLE_GUESSED)) {
            return !hasExecutableDescendant(node);
        }
        return false;
    }

    private static final class State {
        private final String prefix;
        private final boolean webMode;
        private int nextRef = 1;

        State(String prefix, boolean webMode) {
            this.prefix = prefix;
            this.webMode = webMode;
        }

        private SemanticNode assignNode(SemanticNode node) {
            SemanticNode.Builder builder = SemanticNode.builder(node.role())
                    .name(node.name())
                    .text(node.text())
                    .contentDescription(node.contentDescription())
                    .resourceId(node.resourceId())
                    .className(node.className())
                    .bounds(node.bounds())
                    .roleDecision(node.roleDecision())
                    .webElementIdx(node.webElementIdx());
            for (String state : node.states()) {
                builder.addState(state);
            }
            if (shouldAssignRef(node, webMode)) {
                builder.ref(prefix + nextRef++);
            } else if (node.hasRef()) {
                builder.ref(node.ref());
            }
            for (SemanticNode child : node.children()) {
                builder.addChild(assignNode(child));
            }
            return builder.build();
        }

    }

    private static boolean hasExecutableDescendant(SemanticNode node) {
        for (SemanticNode child : node.children()) {
            if (shouldAssignRef(child, false) || hasExecutableDescendant(child)) {
                return true;
            }
        }
        return false;
    }
}
