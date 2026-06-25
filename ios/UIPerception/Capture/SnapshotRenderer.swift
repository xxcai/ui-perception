import Foundation

/// 把 SemanticNode 树渲染成 YAML-like 文本，对应 Android `SnapshotRenderer.java`。
/// 输出格式（缩进 = 深度 × 2 空格）：
/// ```
/// - screen:
///   - toolbar "My App":
///     - button "Submit" [clickable] [ref=n1] [bounds=10,20,100,50]
/// ```
enum SnapshotRenderer {
    static func render(_ root: SemanticNode?, options: SnapshotRenderOptions = .defaults) -> String {
        guard let root else { return "" }
        var output = ""
        appendNode(root, depth: 0, options: options, into: &output)
        return output
    }

    private static func appendNode(_ node: SemanticNode,
                                   depth: Int,
                                   options: SnapshotRenderOptions,
                                   into output: inout String) {
        if options.hasDepthLimit, depth >= options.maxDepth - 1 {
            appendLine(keyFor(node, options: options), depth: depth, into: &output)
            return
        }

        let key = keyFor(node, options: options)
        if node.children.isEmpty {
            appendLine(key, depth: depth, into: &output)
            return
        }

        appendLine(key + ":", depth: depth, into: &output)
        for child in node.children {
            appendNode(child, depth: depth + 1, options: options, into: &output)
        }
    }

    private static func keyFor(_ node: SemanticNode, options: SnapshotRenderOptions) -> String {
        var key = node.role.snapshotName
        if !node.name.isEmpty {
            key += " \"\(escape(node.name))\""
        }
        for state in node.states {
            key += " [\(state)]"
        }
        if node.hasRef {
            key += " [ref=\(node.ref)]"
        }
        if shouldRenderBounds(node, options: options) {
            key += " [bounds=\(node.bounds?.snapshotValue ?? "")]"
        }
        return key
    }

    private static func shouldRenderBounds(_ node: SemanticNode, options: SnapshotRenderOptions) -> Bool {
        guard node.bounds != nil else { return false }
        return node.hasRef || options.boxes
    }

    private static func appendLine(_ text: String, depth: Int, into output: inout String) {
        if !output.isEmpty { output += "\n" }
        output += String(repeating: "  ", count: depth) + "- " + text
    }

    private static func escape(_ value: String) -> String {
        value.replacingOccurrences(of: "\\", with: "\\\\")
             .replacingOccurrences(of: "\"", with: "\\\"")
    }
}

struct SnapshotRenderOptions {
    var boxes: Bool
    var maxDepth: Int

    static let defaults = SnapshotRenderOptions(boxes: false, maxDepth: Int.max)

    var hasDepthLimit: Bool { maxDepth != Int.max }
}
