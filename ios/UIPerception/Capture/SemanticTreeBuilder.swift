import Foundation

/// 把 UIKitViewNode 树转换成 SemanticNode 树。
/// 对应 Android `NativeSemanticTreeBuilder.java` 的简化版：
/// - 走 iOSRoleResolver 推 role
/// - 提取 name / states / bounds
/// - listitem 三档 clickability
/// - RefAssigner 分配 ref
///
/// PoC 阶段先不做 generic 折叠（Android 那条规则只影响输出体积，不影响抓取结果正确性）。
enum SemanticTreeBuilder {

    static func build(from dump: UIKitHierarchyDumper.DumpResult) -> SemanticNode? {
        guard let root = dump.root else { return nil }
        let screenBounds = root.bounds
        let builder = SemanticNode.builder(.screen)
            .className(dump.activityClassName)
            .roleDecision(RoleDecision(.screen, "screen-root", 1.0))
        if let screenBounds { builder.bounds(screenBounds) }

        for child in root.children {
            builder.addChild(buildNode(child))
        }
        let tree = builder.build()
        return RefAssigner.assign(tree, prefix: "n", webMode: false)
    }

    private static func buildNode(_ node: UIKitViewNode) -> SemanticNode {
        let decision = iOSRoleResolver.resolve(node)
        let role = decision.role
        let name = iOSRoleResolver.resolveName(node, role: role)

        let builder = SemanticNode.builder(role)
            .name(name)
            .text(node.text)
            .contentDescription(node.contentDescription)
            .resourceId(node.accessibilityIdentifier)
            .className(node.className)
            .bounds(node.bounds)
            .roleDecision(decision)

        if !node.enabled { builder.addState(SemanticStates.disabled) }
        if node.checked { builder.addState(SemanticStates.checked) }
        if node.selected { builder.addState(SemanticStates.selected) }
        if node.scrollable { builder.addState(SemanticStates.scrollable) }

        if role == .listItem {
            if node.clickable {
                builder.addState(SemanticStates.clickable)
            } else if node.hasGestureRecognizers {
                builder.addState(SemanticStates.clickableInferred)
            } else {
                builder.addState(SemanticStates.clickableGuessed)
            }
        }

        for child in node.children {
            builder.addChild(buildNode(child))
        }
        return builder.build()
    }
}
