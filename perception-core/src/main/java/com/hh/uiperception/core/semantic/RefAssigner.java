package com.hh.uiperception.core.semantic;

/**
 * Assigns snapshot-local ref identifiers to interactive/executable nodes in a semantic tree.
 *
 * Ref strategy differs between web and native pipelines:
 * - Web mode: every node with valid bounds gets a ref (prefix "w1", "w2", ...)
 *   — the JS side already assigns __pr_idx for operation targeting
 * - Native mode: only nodes with interactive roles (button, input, checkbox, etc.)
 *   get refs — avoids polluting the snapshot with refs on non-interactive elements like text/image
 *
 * Special handling for LIST_ITEM in native mode (three-tier clickability logic):
 * 1. Directly clickable or inferred → always assign ref
 * 2. Clickability "guessed" (heuristic) → assign ref only if no executable descendants exist
 *    (avoids double-assigning when a child button already has a ref)
 * 3. Not clickable → no ref
 */
public final class RefAssigner {

    private RefAssigner() {
    }

    /** Assign refs with default prefix "n" in native mode. */
    public static SemanticNode assign(SemanticNode root) {
        return assign(root, "n", false);
    }

    /** Assign refs with custom prefix in native mode. */
    public static SemanticNode assign(SemanticNode root, String prefix) {
        return assign(root, prefix, false);
    }

    /**
     * Assign refs to the tree.
     * @param prefix   ref prefix ("n" for native, "w" for web)
     * @param webMode  true → all nodes with valid bounds get refs; false → only interactive nodes
     */
    public static SemanticNode assign(SemanticNode root, String prefix, boolean webMode) {
        if (root == null) {
            return null;
        }
        return new State(prefix, webMode).assignNode(root);
    }

    /**
     * Decide whether a node should get a ref.
     * Web: any node with valid bounds.
     * Native: role-based check, with special list-item logic.
     */
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

    /** 12 interactive roles that always get refs in native mode. */
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

    /**
     * List-item ref assignment: three-tier clickability logic.
     * - CLICKABLE / CLICKABLE_INFERRED → always assign
     * - CLICKABLE_GUESSED → assign only if no executable descendants (avoid double-assign)
     * - Not clickable → skip
     */
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

    /** Mutable state for recursive traversal: tracks the next ref number. */
    private static final class State {
        private final String prefix;
        private final boolean webMode;
        private int nextRef = 1;

        State(String prefix, boolean webMode) {
            this.prefix = prefix;
            this.webMode = webMode;
        }

        /** Recursively walk the tree, rebuilding each node with an assigned ref if eligible. */
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

    /** Check if any descendant qualifies for a ref (used by list-item guess logic). */
    private static boolean hasExecutableDescendant(SemanticNode node) {
        for (SemanticNode child : node.children()) {
            if (shouldAssignRef(child, false) || hasExecutableDescendant(child)) {
                return true;
            }
        }
        return false;
    }
}
