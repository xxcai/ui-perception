import Foundation

/// 面向模型消费的 native 语义节点，对应 Android `SemanticNode.java`。
final class SemanticNode {
    let role: SemanticRole
    let name: String
    let text: String
    let contentDescription: String
    let resourceId: String
    let className: String
    let bounds: Bounds?
    let ref: String
    let roleDecision: RoleDecision?
    let states: [String]
    let children: [SemanticNode]
    let webElementIdx: Int

    static let noWebElement: Int = -1

    private init(role: SemanticRole,
                 name: String?,
                 text: String?,
                 contentDescription: String?,
                 resourceId: String?,
                 className: String?,
                 bounds: Bounds?,
                 ref: String?,
                 roleDecision: RoleDecision?,
                 states: [String],
                 children: [SemanticNode],
                 webElementIdx: Int) {
        self.role = role
        self.name = SemanticNode.normalize(name)
        self.text = SemanticNode.normalize(text)
        self.contentDescription = SemanticNode.normalize(contentDescription)
        self.resourceId = SemanticNode.normalize(resourceId)
        self.className = SemanticNode.normalize(className)
        self.bounds = bounds
        self.ref = SemanticNode.normalize(ref)
        self.roleDecision = roleDecision
        self.states = states
        self.children = children
        self.webElementIdx = webElementIdx
    }

    var hasRef: Bool { !ref.isEmpty }

    static func builder(_ role: SemanticRole) -> Builder { Builder(role) }

    private static func normalize(_ value: String?) -> String {
        guard let value else { return "" }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        let collapsed = trimmed.replacingOccurrences(
            of: "\\s+",
            with: " ",
            options: .regularExpression
        )
        return collapsed
    }

    final class Builder {
        private let role: SemanticRole
        private var name: String?
        private var text: String?
        private var contentDescription: String?
        private var resourceId: String?
        private var className: String?
        private var bounds: Bounds?
        private var ref: String?
        private var roleDecision: RoleDecision?
        private var states: [String] = []
        private var children: [SemanticNode] = []
        private var webElementIdx: Int = SemanticNode.noWebElement

        init(_ role: SemanticRole) { self.role = role }

        @discardableResult
        func name(_ v: String?) -> Builder { self.name = v; return self }

        @discardableResult
        func text(_ v: String?) -> Builder { self.text = v; return self }

        @discardableResult
        func contentDescription(_ v: String?) -> Builder { self.contentDescription = v; return self }

        @discardableResult
        func resourceId(_ v: String?) -> Builder { self.resourceId = v; return self }

        @discardableResult
        func className(_ v: String?) -> Builder { self.className = v; return self }

        @discardableResult
        func bounds(_ v: Bounds?) -> Builder { self.bounds = v; return self }

        @discardableResult
        func ref(_ v: String?) -> Builder { self.ref = v; return self }

        @discardableResult
        func roleDecision(_ v: RoleDecision?) -> Builder { self.roleDecision = v; return self }

        @discardableResult
        func addState(_ v: String?) -> Builder {
            let normalized = SemanticNode.normalize(v)
            if !normalized.isEmpty { states.append(normalized) }
            return self
        }

        @discardableResult
        func addChild(_ child: SemanticNode?) -> Builder {
            if let child { children.append(child) }
            return self
        }

        @discardableResult
        func webElementIdx(_ v: Int) -> Builder { self.webElementIdx = v; return self }

        func build() -> SemanticNode {
            SemanticNode(
                role: role,
                name: name,
                text: text,
                contentDescription: contentDescription,
                resourceId: resourceId,
                className: className,
                bounds: bounds,
                ref: ref,
                roleDecision: roleDecision,
                states: states,
                children: children,
                webElementIdx: webElementIdx
            )
        }
    }
}
