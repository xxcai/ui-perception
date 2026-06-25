import UIKit

/// iOS UIKit class → SemanticRole 映射，对应 Android `NativeRoleResolver.java`。
/// 规则集中在这里，方便后续版本化演进。
/// ROLE_VERSION 与 Android `android-role-v1` 区分，便于跨端识别来源。
enum iOSRoleResolver {
    static let roleVersion = "ios-uikit-role-v1"

    static func resolve(_ node: UIKitViewNode) -> RoleDecision {
        let className = node.className
        let simpleName = self.simpleName(className)

        if let classDecision = resolveByClass(simpleName: simpleName, className: className, node: node) {
            if let adjusted = adjustByAttributes(node: node, classDecision: classDecision) {
                return adjusted
            }
            return classDecision
        }
        if node.hasTextOrLabel {
            return RoleDecision(.text, "text-or-label-fallback", 0.55)
        }
        return RoleDecision(.generic, "generic-fallback", 0.3)
    }

    static func resolveName(_ node: UIKitViewNode, role: SemanticRole) -> String {
        if role == .input {
            if !node.hint.isEmpty { return node.hint }
            if !node.contentDescription.isEmpty { return node.contentDescription }
            if !node.accessibilityIdentifier.isEmpty {
                return readableResourceName(node.accessibilityIdentifier)
            }
            return ""
        }
        if !node.text.isEmpty { return node.text }
        if !node.contentDescription.isEmpty { return node.contentDescription }
        if role != .text, !node.accessibilityIdentifier.isEmpty {
            return readableResourceName(node.accessibilityIdentifier)
        }
        return ""
    }

    private static func resolveByClass(simpleName: String, className: String, node: UIKitViewNode) -> RoleDecision? {
        guard !simpleName.isEmpty else { return nil }

        if simpleName == "UIWindow" || simpleName == "UIDropShadowView" {
            return RoleDecision(.screen, "class:root", 0.9)
        }
        if containsAny(simpleName, "WKWebView", "UIWebView") {
            return RoleDecision(.webview, "class:webview", 0.9)
        }
        if containsAny(simpleName, "UINavigationBar", "UIToolbar", "UINavigationBarView") {
            return RoleDecision(.toolbar, "class:toolbar", 0.9)
        }
        if containsAny(simpleName, "UITabBar") {
            return RoleDecision(.tabbar, "class:tabbar", 0.9)
        }
        if containsAny(simpleName, "UISegmentedControl") {
            return RoleDecision(.tabbar, "class:tabbar", 0.85)
        }
        if containsAny(simpleName, "UICollectionView") {
            return RoleDecision(.grid, "class:grid", 0.9)
        }
        if containsAny(simpleName, "UITableView") {
            return RoleDecision(.list, "class:list", 0.9)
        }
        if containsAny(simpleName, "UITableViewCell", "UICollectionViewCell", "UIContextMenuCell") {
            return RoleDecision(.listItem, "class:listitem", 0.9)
        }
        if containsAny(simpleName, "UIScrollView") {
            return RoleDecision(.scroll, "class:scroll", 0.9)
        }
        if containsAny(simpleName, "UIAlertController", "UIAlertView", "UIActionSheet") {
            return RoleDecision(.dialog, "class:dialog", 0.85)
        }
        if containsAny(simpleName, "UITextField", "UITextView") {
            return RoleDecision(.input, "class:input", 0.95)
        }
        if containsAny(simpleName, "UISwitch") {
            return RoleDecision(.switchRole, "class:switch", 0.95)
        }
        if containsAny(simpleName, "UISlider", "UISegmentedControl") {
            return RoleDecision(.slider, "class:slider", 0.9)
        }
        if containsAny(simpleName, "UIPickerView", "UIDatePicker") {
            return RoleDecision(.picker, "class:picker", 0.9)
        }
        if containsAny(simpleName, "UIProgressView", "UIActivityIndicatorView", "NSProgressView") {
            return RoleDecision(.progress, "class:progress", 0.9)
        }
        if containsAny(simpleName, "UIButton") {
            return RoleDecision(.button, "class:button", 0.95)
        }
        if containsAny(simpleName, "UIImageView") {
            return RoleDecision(.image, "class:image", 0.85)
        }
        if containsAny(simpleName, "UILabel", "UITextView") {
            return RoleDecision(.text, "class:text", 0.85)
        }
        // 兜底容器
        if containsAny(simpleName, "UIStackView", "UIView", "UIControl", "UIContentView",
                       "ContentAreaView", "ContainerView", "BackgroundView", "WrapperView") {
            return RoleDecision(.generic, "class:container", 0.6)
        }
        return nil
    }

    private static func adjustByAttributes(node: UIKitViewNode, classDecision: RoleDecision) -> RoleDecision? {
        if classDecision.source == "class:webview" { return nil }

        let role = classDecision.role
        let interactiveGeneric: Bool = role == .text || role == .image || role == .generic
        if (node.clickable || node.hasGestureRecognizers) && interactiveGeneric {
            return RoleDecision(.button, "attribute:clickable", 0.8)
        }
        if node.scrollable, role == .generic {
            return RoleDecision(.scroll, "attribute:scrollable", 0.7)
        }
        return nil
    }

    private static func simpleName(_ className: String) -> String {
        guard !className.isEmpty else { return "" }
        if let dot = className.lastIndex(of: ".") {
            return String(className[className.index(after: dot)...])
        }
        return className
    }

    private static func containsAny(_ value: String, _ needles: String...) -> Bool {
        for needle in needles where value.contains(needle) { return true }
        return false
    }

    private static func readableResourceName(_ id: String) -> String {
        let tail = id.split(separator: "/").last.map(String.init) ?? id
        return tail.replacingOccurrences(of: "_", with: " ").trimmingCharacters(in: .whitespaces)
    }
}
