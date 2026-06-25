import Foundation

/// Web DOM 树归一化：generic 折叠 + 连续文本合并 + 冗余文本去除。
/// 对应 Android `TreeNormalizer.java`。
///
/// Playwright ref:
/// - normalizeGenericRoles: ariaSnapshot.ts:290-311
/// - normalizeStringChildren: ariaSnapshot.ts:313-341
enum TreeNormalizer {

    static func normalize(_ root: SemanticNode?) -> SemanticNode? {
        guard let root else { return nil }
        return normalizeNode(root)
    }

    private static func normalizeNode(_ node: SemanticNode) -> SemanticNode {
        var normalized: [SemanticNode] = []
        for child in node.children {
            normalized.append(normalizeNode(child))
        }

        // 连续 text 子节点合并（先于 generic 折叠，合并后可能触发折叠）
        normalized = mergeConsecutiveTexts(normalized)

        // Generic 折叠：无名称、无 states、无 ref、至多 1 个子节点 → 解包
        if node.role == .generic,
           node.name.isEmpty,
           node.states.isEmpty,
           !node.hasRef,
           normalized.count <= 1 {
            return normalized.isEmpty ? node : normalized[0]
        }

        // 冗余文本去除：唯一子节点是 text 且内容等于 name → 移除
        if normalized.count == 1,
           normalized[0].role == .text,
           !node.name.isEmpty,
           node.name == normalized[0].name {
            normalized = []
        }

        if sameChildren(node.children, normalized) {
            return node
        }

        let builder = SemanticNode.builder(node.role)
            .name(node.name)
            .text(node.text)
            .contentDescription(node.contentDescription)
            .resourceId(node.resourceId)
            .className(node.className)
            .bounds(node.bounds)
            .roleDecision(node.roleDecision)
            .webElementIdx(node.webElementIdx)
        if node.hasRef { builder.ref(node.ref) }
        for state in node.states { builder.addState(state) }
        for child in normalized { builder.addChild(child) }
        return builder.build()
    }

    private static func mergeConsecutiveTexts(_ children: [SemanticNode]) -> [SemanticNode] {
        if children.count <= 1 { return children }

        var result: [SemanticNode] = []
        var textGroup: SemanticNode? = nil

        for child in children {
            if child.role == .text {
                if textGroup == nil {
                    textGroup = child
                } else {
                    let merged = textGroup!.name + " " + child.name
                    textGroup = SemanticNode.builder(.text)
                        .name(merged)
                        .bounds(textGroup!.bounds)
                        .webElementIdx(textGroup!.webElementIdx)
                        .build()
                }
            } else {
                if let group = textGroup {
                    result.append(group)
                    textGroup = nil
                }
                result.append(child)
            }
        }
        if let group = textGroup { result.append(group) }
        return result
    }

    private static func sameChildren(_ a: [SemanticNode], _ b: [SemanticNode]) -> Bool {
        guard a.count == b.count else { return false }
        for i in 0..<a.count {
            if a[i] !== b[i] { return false }
        }
        return true
    }
}
