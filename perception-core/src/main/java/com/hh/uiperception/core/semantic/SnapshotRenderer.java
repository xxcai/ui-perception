package com.hh.uiperception.core.semantic;

/**
 * Renders a SemanticNode tree into a YAML-like text snapshot.
 *
 * Output format — indentation reflects tree depth, each node is a bullet point:
 * <pre>
 * - screen:
 *   - toolbar "My App":
 *     - button "Submit" [clickable] [ref=n1] [bounds=10,20,100,50]
 *     - text "Hello world"
 * </pre>
 *
 * Line format: {@code role "name" [state1] [state2] [ref=nX] [bounds=x1,y1,x2,y2]}
 * - Parent nodes end with ":"
 * - Leaf nodes are flat
 * - Bounds are only rendered for nodes with a ref or when the boxes option is enabled
 */
public final class SnapshotRenderer {

    private SnapshotRenderer() {
    }

    public static String render(SemanticNode root) {
        return render(root, SnapshotRenderOptions.defaults());
    }

    public static String render(SemanticNode root, SnapshotRenderOptions options) {
        if (root == null) {
            return "";
        }
        SnapshotRenderOptions actualOptions = options == null
                ? SnapshotRenderOptions.defaults() : options;
        StringBuilder builder = new StringBuilder();
        appendNode(builder, root, 0, actualOptions);
        return builder.toString();
    }

    /**
     * Recursively render a node and its children.
     * Leaf nodes render as a single line; parent nodes append ":" and recurse.
     */
    private static void appendNode(StringBuilder builder, SemanticNode node,
                                   int depth, SnapshotRenderOptions options) {
        if (options.hasDepthLimit() && depth >= options.maxDepth() - 1) {
            appendLine(builder, depth, keyFor(node, options));
            return;
        }

        String key = keyFor(node, options);
        if (node.children().isEmpty()) {
            appendLine(builder, depth, key);
            return;
        }

        appendLine(builder, depth, key + ":");
        for (SemanticNode child : node.children()) {
            appendNode(builder, child, depth + 1, options);
        }
    }

    /**
     * Build the display string for a single node.
     * Format: {@code role "name" [state1] [state2] [ref=nX] [bounds=x1,y1,x2,y2]}
     */
    private static String keyFor(SemanticNode node, SnapshotRenderOptions options) {
        StringBuilder key = new StringBuilder();
        key.append(node.role().snapshotName());
        if (!node.name().isEmpty()) {
            key.append(" \"").append(escape(node.name())).append("\"");
        }
        for (String state : node.states()) {
            key.append(" [").append(renderState(state)).append("]");
        }
        if (node.hasRef()) {
            key.append(" [ref=").append(node.ref()).append("]");
        }
        if (shouldRenderBounds(node, options)) {
            key.append(" [bounds=").append(node.bounds().toSnapshotValue()).append("]");
        }
        return key.toString();
    }

    /** Bounds are rendered only if the node has a ref or the boxes option is enabled. */
    private static boolean shouldRenderBounds(SemanticNode node, SnapshotRenderOptions options) {
        return node.bounds() != null && (node.hasRef() || options.boxes());
    }

    private static String renderState(String state) {
        return state;
    }

    private static void appendLine(StringBuilder builder, int depth, String text) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append("  ".repeat(depth)).append("- ").append(text);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
