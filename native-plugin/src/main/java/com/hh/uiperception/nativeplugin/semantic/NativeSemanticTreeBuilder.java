package com.hh.uiperception.nativeplugin.semantic;

/**
 * 将原始 ViewNode 树转换为 native semantic tree。
 */
public final class NativeSemanticTreeBuilder {

    private NativeSemanticTreeBuilder() {
    }

    public static NativeSemanticNode build(NativeViewNode root) {
        if (root == null) {
            return null;
        }
        return buildNode(root, null);
    }

    private static NativeSemanticNode buildNode(NativeViewNode viewNode, NativeSemanticRole parentRole) {
        NativeRoleDecision roleDecision = NativeRoleResolver.resolve(viewNode);
        NativeSemanticRole role = roleDecision.role();
        if (shouldTreatAsCollectionItem(parentRole, role)) {
            role = NativeSemanticRole.LIST_ITEM;
            roleDecision = new NativeRoleDecision(role, "structure:collection-item", 0.75);
        }
        String name = NativeRoleResolver.resolveName(viewNode, role);

        NativeSemanticNode.Builder builder = NativeSemanticNode.builder(role)
                .name(name)
                .text(viewNode.text())
                .contentDescription(viewNode.contentDescription())
                .resourceId(viewNode.resourceId())
                .className(viewNode.className())
                .bounds(viewNode.bounds())
                .roleDecision(roleDecision);
        appendStates(builder, viewNode);
        appendVisualDescriptionState(builder, viewNode, role, name);

        for (NativeViewNode child : viewNode.children()) {
            NativeSemanticNode semanticChild = buildNode(child, role);
            if (semanticChild != null) {
                builder.addChild(semanticChild);
            }
        }

        NativeSemanticNode node = builder.build();
        return shouldFoldGeneric(node) ? node.children().get(0) : node;
    }

    private static boolean shouldTreatAsCollectionItem(NativeSemanticRole parentRole, NativeSemanticRole role) {
        if (parentRole != NativeSemanticRole.LIST && parentRole != NativeSemanticRole.GRID) {
            return false;
        }
        return role == NativeSemanticRole.GENERIC
                || role == NativeSemanticRole.CARD
                || role == NativeSemanticRole.SECTION;
    }

    private static void appendStates(NativeSemanticNode.Builder builder, NativeViewNode viewNode) {
        if (!viewNode.enabled()) {
            builder.addState("disabled");
        }
        if (viewNode.checked()) {
            builder.addState("checked");
        }
        if (viewNode.selected()) {
            builder.addState("selected");
        }
        if (viewNode.focused()) {
            builder.addState("focused");
        }
        if (viewNode.scrollable()) {
            builder.addState("scrollable");
        }
        if (viewNode.password()) {
            builder.addState("password");
        }
    }

    private static void appendVisualDescriptionState(
            NativeSemanticNode.Builder builder,
            NativeViewNode viewNode,
            NativeSemanticRole role,
            String name
    ) {
        NativeBounds bounds = viewNode.bounds();
        if (role == NativeSemanticRole.BUTTON
                && name.isEmpty()
                && isButtonClass(viewNode.className())
                && bounds != null
                && bounds.isValid()) {
            builder.addState("needs_visual_desc");
        }
    }

    private static boolean isButtonClass(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        int index = className.lastIndexOf('.');
        String simpleName = index >= 0 ? className.substring(index + 1) : className;
        return simpleName.contains("Button");
    }

    private static boolean shouldFoldGeneric(NativeSemanticNode node) {
        return node.role() == NativeSemanticRole.GENERIC
                && node.name().isEmpty()
                && node.states().isEmpty()
                && !node.hasRef()
                && node.children().size() == 1;
    }
}
