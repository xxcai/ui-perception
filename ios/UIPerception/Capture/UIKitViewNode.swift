import UIKit

/// UIView 节点的中间表示，对应 Android `NativeViewNode.java`。
/// 由 `UIKitHierarchyDumper` 在递归遍历时填充，由 `iOSRoleResolver` / `SemanticTreeBuilder` 消费。
struct UIKitViewNode {
    let className: String
    let accessibilityIdentifier: String
    let text: String
    let hint: String
    let contentDescription: String
    let boundsString: String          // "[l,t][r,b]"，与 Android 一致
    let bounds: Bounds?
    let clickable: Bool               // view is UIControl 或 accessibilityTraits 含 .button
    let hasGestureRecognizers: Bool   // view.gestureRecognizers 非空
    let enabled: Bool
    let focusable: Bool
    let checked: Bool
    let scrollable: Bool
    let selected: Bool
    let children: [UIKitViewNode]

    var hasTextOrLabel: Bool { !text.isEmpty || !contentDescription.isEmpty }
}

/// UIKit View 树遍历器，对应 Android `ViewHierarchyDumper.java`。
/// 从 keyWindow.rootViewController.view 开始递归，输出 UIKitViewNode 树。
/// 必须在主线程调用。
enum UIKitHierarchyDumper {

    struct DumpResult {
        let activityClassName: String
        let root: UIKitViewNode?
        let nodeCount: Int
    }

    static func dump() -> DumpResult {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive })
            ?? UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first
        else {
            return DumpResult(activityClassName: "no-scene", root: nil, nodeCount: 0)
        }

        // 跳过 alert 级及以上的 window（浮动按钮 window 在 .alert + 1），
        // 同时跳过隐私保护用的 keyboard window 等。优先 .normal 级别且是 key 的窗口。
        let appWindows = scene.windows.filter { $0.windowLevel == .normal }
        guard let targetWindow = appWindows.first(where: { $0.isKeyWindow })
            ?? appWindows.first(where: { !$0.isHidden })
            ?? appWindows.first
        else {
            return DumpResult(activityClassName: "no-window", root: nil, nodeCount: 0)
        }

        let activityName = activityClassName(from: targetWindow.rootViewController)
        var counter = 0
        let root = appendNode(targetWindow, counter: &counter)
        return DumpResult(activityClassName: activityName, root: root, nodeCount: counter)
    }

    private static func activityClassName(from viewController: UIViewController?) -> String {
        guard let initial = viewController else { return "unknown" }
        var current: UIViewController = initial
        while let parent = current.parent {
            current = parent
        }
        if let nav = current as? UINavigationController, let top = nav.visibleViewController {
            return String(describing: type(of: top))
        }
        return String(describing: type(of: current))
    }

    private static func appendNode(_ view: UIView, counter: inout Int) -> UIKitViewNode? {
        if !isMeaningful(view) { return nil }
        counter += 1

        let bounds = extractBounds(view)
        let className = String(describing: type(of: view))
        let childNodes = view.subviews.compactMap { appendNode($0, counter: &counter) }

        return UIKitViewNode(
            className: className,
            accessibilityIdentifier: view.accessibilityIdentifier ?? "",
            text: extractText(view),
            hint: extractHint(view),
            contentDescription: view.accessibilityLabel ?? "",
            boundsString: formatBounds(bounds),
            bounds: bounds,
            clickable: isClickable(view),
            hasGestureRecognizers: !(view.gestureRecognizers?.isEmpty ?? true),
            enabled: view.isUserInteractionEnabled,
            focusable: view.canBecomeFirstResponder,
            checked: isChecked(view),
            scrollable: view is UIScrollView,
            selected: view.accessibilityTraits.contains(.selected),
            children: childNodes
        )
    }

    private static func isMeaningful(_ view: UIView) -> Bool {
        guard !view.isHidden, view.alpha > 0 else { return false }
        guard view.bounds.width > 0, view.bounds.height > 0 else { return false }
        return true
    }

    private static func extractText(_ view: UIView) -> String {
        if let label = view as? UILabel, let text = label.text, !text.isEmpty { return text }
        if let button = view as? UIButton, let title = button.title(for: .normal), !title.isEmpty {
            return title
        }
        if let textField = view as? UITextField, let text = textField.text, !text.isEmpty {
            return text
        }
        if let textView = view as? UITextView, !textView.text.isEmpty {
            return textView.text
        }
        return ""
    }

    private static func extractHint(_ view: UIView) -> String {
        if let textField = view as? UITextField, let placeholder = textField.placeholder, !placeholder.isEmpty {
            return placeholder
        }
        if let textView = view as? UITextView, !textView.text.isEmpty {
            // UITextView 没有原生 placeholder，开发者通常自绘
            return ""
        }
        return ""
    }

    private static func extractBounds(_ view: UIView) -> Bounds? {
        guard let window = view.window else {
            let frame = view.frame
            return Bounds(CGRect(x: frame.minX, y: frame.minY, width: frame.width, height: frame.height))
        }
        let frameInWindow = view.convert(view.bounds, to: window)
        return Bounds(frameInWindow)
    }

    private static func formatBounds(_ bounds: Bounds?) -> String {
        guard let bounds else { return "" }
        return "[\(bounds.left),\(bounds.top)][\(bounds.right),\(bounds.bottom)]"
    }

    private static func isClickable(_ view: UIView) -> Bool {
        // 注意：不能靠 accessibilityTraits.contains(.button) 判定 ——
        // iOS 会把父控件（如 UIButton）的 accessibilityTraits 透传给子 UILabel/UIImageView，
        // 导致按钮里的 title label 被误判为 button。
        // 只信任：UIControl 子类（UIButton/UISwitch/...）或显式挂的 gestureRecognizer。
        if view is UIControl { return true }
        if !(view.gestureRecognizers?.isEmpty ?? true) { return true }
        return false
    }

    private static func isChecked(_ view: UIView) -> Bool {
        if let toggle = view as? UISwitch { return toggle.isOn }
        if view.accessibilityTraits.contains(.selected) { return true }
        return false
    }
}
