import Foundation

/// 给语义树中的可交互节点分配 snapshot-local ref 标识。
/// 对应 Android `RefAssigner.java`：
/// - native mode：只有可交互角色（button/input/...）+ 三档 listitem 逻辑才分配
/// - web mode：所有有 bounds 的节点都分配
enum RefAssigner {
    static func assign(_ root: SemanticNode?, prefix: String = "n", webMode: Bool = false) -> SemanticNode? {
        guard let root else { return nil }
        return State(prefix: prefix, webMode: webMode).assignNode(root)
    }

    private static func shouldAssignRef(_ node: SemanticNode, webMode: Bool) -> Bool {
        guard let bounds = node.bounds, bounds.isValid else { return false }
        if webMode { return true }
        if node.role == .listItem {
            return shouldAssignRefToListItem(node)
        }
        return isExecutableRole(node.role)
    }

    private static func isExecutableRole(_ role: SemanticRole) -> Bool {
        switch role {
        case .button, .input, .checkbox, .radio, .switchRole,
             .slider, .picker, .list, .grid, .scroll, .webview:
            return true
        default:
            return false
        }
    }

    private static func shouldAssignRefToListItem(_ node: SemanticNode) -> Bool {
        if node.states.contains(SemanticStates.clickable)
            || node.states.contains(SemanticStates.clickableInferred) {
            return true
        }
        if node.states.contains(SemanticStates.clickableGuessed) {
            return !hasExecutableDescendant(node)
        }
        return false
    }

    private static func hasExecutableDescendant(_ node: SemanticNode) -> Bool {
        for child in node.children {
            if shouldAssignRef(child, webMode: false) || hasExecutableDescendant(child) {
                return true
            }
        }
        return false
    }

    private final class State {
        let prefix: String
        let webMode: Bool
        var nextRef: Int = 1

        init(prefix: String, webMode: Bool) {
            self.prefix = prefix
            self.webMode = webMode
        }

        func assignNode(_ node: SemanticNode) -> SemanticNode {
            let builder = SemanticNode.builder(node.role)
                .name(node.name)
                .text(node.text)
                .contentDescription(node.contentDescription)
                .resourceId(node.resourceId)
                .className(node.className)
                .bounds(node.bounds)
                .roleDecision(node.roleDecision)
                .webElementIdx(node.webElementIdx)
            for state in node.states {
                builder.addState(state)
            }
            if RefAssigner.shouldAssignRef(node, webMode: webMode) {
                builder.ref(prefix + String(nextRef))
                nextRef += 1
            } else if node.hasRef {
                builder.ref(node.ref)
            }
            for child in node.children {
                builder.addChild(assignNode(child))
            }
            return builder.build()
        }
    }
}

enum SemanticStates {
    static let clickable = "clickable"
    static let clickableInferred = "clickable-inferred"
    static let clickableGuessed = "clickable-guessed"
    static let disabled = "disabled"
    static let checked = "checked"
    static let selected = "selected"
    static let focused = "focused"
    static let scrollable = "scrollable"
    static let password = "password"
}
