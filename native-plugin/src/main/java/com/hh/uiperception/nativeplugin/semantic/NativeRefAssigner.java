package com.hh.uiperception.nativeplugin.semantic;

/**
 * 为 semantic tree 中可执行节点分配当前 snapshot 内 ref。
 */
public final class NativeRefAssigner {

    private NativeRefAssigner() {
    }

    public static NativeSemanticNode assign(NativeSemanticNode root) {
        if (root == null) {
            return null;
        }
        return new State().assignNode(root);
    }

    private static boolean shouldAssignRef(NativeSemanticNode node) {
        return node.bounds() != null
                && node.bounds().isValid()
                && isExecutableRole(node.role());
    }

    private static boolean isExecutableRole(NativeSemanticRole role) {
        return role == NativeSemanticRole.BUTTON
                || role == NativeSemanticRole.INPUT
                || role == NativeSemanticRole.CHECKBOX
                || role == NativeSemanticRole.RADIO
                || role == NativeSemanticRole.SWITCH
                || role == NativeSemanticRole.SLIDER
                || role == NativeSemanticRole.PICKER
                || role == NativeSemanticRole.LIST
                || role == NativeSemanticRole.GRID
                || role == NativeSemanticRole.SCROLL;
    }

    private static final class State {
        private int nextRef = 1;

        private NativeSemanticNode assignNode(NativeSemanticNode node) {
            NativeSemanticNode.Builder builder = NativeSemanticNode.builder(node.role())
                    .name(node.name())
                    .text(node.text())
                    .contentDescription(node.contentDescription())
                    .resourceId(node.resourceId())
                    .className(node.className())
                    .bounds(node.bounds())
                    .roleDecision(node.roleDecision());
            for (String state : node.states()) {
                builder.addState(state);
            }
            if (shouldAssignRef(node)) {
                builder.ref("n" + nextRef++);
            } else if (node.hasRef()) {
                builder.ref(node.ref());
            }
            for (NativeSemanticNode child : node.children()) {
                builder.addChild(assignNode(child));
            }
            return builder.build();
        }
    }
}
