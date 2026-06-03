package com.hh.uiperception.core.semantic;

/**
 * 将 native semantic tree 渲染为 YAML 风格文本。
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
