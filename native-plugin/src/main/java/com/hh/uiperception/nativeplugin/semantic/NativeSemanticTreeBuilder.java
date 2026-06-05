package com.hh.uiperception.nativeplugin.semantic;

import com.hh.uiperception.core.semantic.*;

/**
 * 将原始 ViewNode 树转换为 native semantic tree。
 */
public final class NativeSemanticTreeBuilder {

    private NativeSemanticTreeBuilder() {
    }

    public static SemanticNode build(NativeViewNode root) {
        if (root == null) {
            return null;
        }
        return buildNode(root, null, false, false);
    }

    private static SemanticNode buildNode(
            NativeViewNode viewNode,
            SemanticRole parentRole,
            boolean parentHasItemClickListener,
            boolean parentHasItemTouchListener
    ) {
        RoleDecision roleDecision = NativeRoleResolver.resolve(viewNode);
        SemanticRole role = roleDecision.role();
        if (shouldTreatAsCollectionItem(parentRole, roleDecision)) {
            role = SemanticRole.LIST_ITEM;
            roleDecision = new RoleDecision(role, collectionItemSource(roleDecision), 0.75);
        }
        String name = NativeRoleResolver.resolveName(viewNode, role);

        SemanticNode.Builder builder = SemanticNode.builder(role)
                .name(name)
                .text(viewNode.text())
                .contentDescription(viewNode.contentDescription())
                .resourceId(viewNode.resourceId())
                .className(viewNode.className())
                .bounds(viewNode.bounds())
                .roleDecision(roleDecision);
        appendStates(builder, viewNode, role);
        appendClickableState(builder, role, viewNode,
                parentHasItemClickListener, parentHasItemTouchListener);
        appendVisualDescriptionState(builder, viewNode, role, name);

        for (NativeViewNode child : viewNode.children()) {
            SemanticNode semanticChild = buildNode(child, role,
                    viewNode.hasItemClickListener(), viewNode.hasItemTouchListener());
            if (semanticChild != null) {
                builder.addChild(semanticChild);
            }
        }

        SemanticNode node = builder.build();
        return shouldFoldGeneric(node) ? node.children().get(0) : node;
    }

    private static boolean shouldTreatAsCollectionItem(
            SemanticRole parentRole,
            RoleDecision roleDecision
    ) {
        if (parentRole != SemanticRole.LIST && parentRole != SemanticRole.GRID) {
            return false;
        }
        SemanticRole role = roleDecision.role();
        return role == SemanticRole.GENERIC
                || role == SemanticRole.CARD
                || role == SemanticRole.SECTION
                || isClickableGenericRole(roleDecision);
    }

    private static boolean isClickableGenericRole(RoleDecision roleDecision) {
        return roleDecision.role() == SemanticRole.BUTTON
                && "attribute:clickable".equals(roleDecision.source());
    }

    private static String collectionItemSource(RoleDecision roleDecision) {
        if (isClickableGenericRole(roleDecision)) {
            return "structure:collection-item-clickable";
        }
        return "structure:collection-item";
    }

    private static void appendStates(SemanticNode.Builder builder, NativeViewNode viewNode, SemanticRole role) {
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
        if (role == SemanticRole.INPUT && viewNode.hasText()) {
            builder.addState("value=" + viewNode.text());
        }
    }

    private static void appendClickableState(
            SemanticNode.Builder builder,
            SemanticRole role,
            NativeViewNode viewNode,
            boolean parentHasItemClickListener,
            boolean parentHasItemTouchListener
    ) {
        String clickableState = resolveClickableState(role, viewNode,
                parentHasItemClickListener, parentHasItemTouchListener);
        if (clickableState != null) {
            builder.addState(clickableState);
        }
    }

    private static String resolveClickableState(
            SemanticRole role,
            NativeViewNode viewNode,
            boolean parentHasItemClickListener,
            boolean parentHasItemTouchListener
    ) {
        if (role != SemanticRole.LIST_ITEM) {
            return null;
        }
        if (viewNode.clickable() || viewNode.hasOnClickListener() || viewNode.hasTouchOverride()) {
            return SemanticStates.CLICKABLE;
        }
        if (parentHasItemClickListener || parentHasItemTouchListener) {
            return SemanticStates.CLICKABLE_INFERRED;
        }
        return SemanticStates.CLICKABLE_GUESSED;
    }

    private static void appendVisualDescriptionState(
            SemanticNode.Builder builder,
            NativeViewNode viewNode,
            SemanticRole role,
            String name
    ) {
        Bounds bounds = viewNode.bounds();
        if (role == SemanticRole.BUTTON
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

    private static boolean shouldFoldGeneric(SemanticNode node) {
        return node.role() == SemanticRole.GENERIC
                && node.name().isEmpty()
                && node.states().isEmpty()
                && !node.hasRef()
                && node.children().size() == 1;
    }
}
