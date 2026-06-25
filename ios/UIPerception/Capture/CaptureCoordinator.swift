import UIKit
import WebKit

/// capture 入口。根据当前可见 VC 路由到 native 或 web 抓取流程。
enum CaptureCoordinator {

    struct CaptureResult {
        let summary: String
        let outputURL: URL?
    }

    @discardableResult
    static func capture(baselineId: String? = nil) -> CaptureResult {
        // 先看当前 VC 是不是 Web VC，是的话走 web 抓取；否则走 native
        if currentViewController() is WebBaselineViewController {
            let web = WebCapture.capture()
            presentResult(web.summary, url: web.outputURL)
            return CaptureResult(summary: web.summary, outputURL: web.outputURL)
        }

        // 也可以更通用：检测当前 view 里有没有 WKWebView
        if WebViewFinder.findCurrent() != nil {
            let web = WebCapture.capture()
            presentResult(web.summary, url: web.outputURL)
            return CaptureResult(summary: web.summary, outputURL: web.outputURL)
        }

        let nativeResult = captureNative(baselineId: baselineId)
        presentResult(nativeResult.summary, url: nativeResult.outputURL)
        return nativeResult
    }

    /// Native（UIView 树）抓取。对应 Android `NativePerceptionPlugin.capture + transform`。
    private static func captureNative(baselineId: String?) -> CaptureResult {
        let dump = UIKitHierarchyDumper.dump()
        guard let tree = SemanticTreeBuilder.build(from: dump) else {
            return CaptureResult(summary: "抓取失败：无法构造语义树", outputURL: nil)
        }
        let snapshot = SnapshotRenderer.render(tree)
        let nodeSummary = summarize(tree)

        let baseline = baselineId ?? inferNativeBaselineId(from: dump.activityClassName)
        let timestamp = Int(Date().timeIntervalSince1970 * 1000)
        let runDir = documentsDirectory
            .appendingPathComponent("captures")
            .appendingPathComponent(baseline)
            .appendingPathComponent("runs")
            .appendingPathComponent(String(timestamp))
            .appendingPathComponent("native")
        let rawDir = runDir.appendingPathComponent("raw")
        let transformedDir = runDir.appendingPathComponent("transformed")
        try? FileManager.default.createDirectory(at: rawDir, withIntermediateDirectories: true)
        try? FileManager.default.createDirectory(at: transformedDir, withIntermediateDirectories: true)

        let rawPath = rawDir.appendingPathComponent("raw_dump_\(timestamp).json")
        let transformedPath = transformedDir.appendingPathComponent("native_semantic_snapshot_\(timestamp).yml")

        let rawPayload: [String: Any] = [
            "activity": dump.activityClassName,
            "nodeCount": dump.nodeCount,
            "capturedAt": ISO8601DateFormatter().string(from: Date()),
        ]
        let rawData = try? JSONSerialization.data(
            withJSONObject: rawPayload,
            options: [.prettyPrinted, .sortedKeys]
        )
        try? rawData?.write(to: rawPath)
        try? snapshot.data(using: .utf8)?.write(to: transformedPath)

        let summary = """
        \(nodeSummary)
        节点数：\(dump.nodeCount)
        Activity：\(dump.activityClassName)
        baseline：\(baseline)
        文件：\(transformedPath.lastPathComponent)
        """
        return CaptureResult(summary: summary, outputURL: transformedPath)
    }

    static func shareFile(_ url: URL) {
        let activator = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }) ??
            UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first,
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else { return }
        if let pop = activator.popoverPresentationController {
            pop.sourceView = root.view
            pop.sourceRect = CGRect(
                x: root.view.bounds.midX,
                y: root.view.bounds.midY,
                width: 0,
                height: 0
            )
            pop.permittedArrowDirections = []
        }
        root.topMost().present(activator, animated: true)
    }

    private static func presentResult(_ summary: String, url: URL?) {
        let alert = UIAlertController(title: "抓取完成", message: summary, preferredStyle: .alert)
        if let url {
            alert.addAction(UIAlertAction(title: "分享文件", style: .default) { _ in
                shareFile(url)
            })
        }
        alert.addAction(UIAlertAction(title: "确定", style: .cancel))
        // 浮动按钮 window 可能抢占 key，currentViewController() 已做 fallback
        currentViewController()?.topMost().present(alert, animated: true)
    }

    private static func currentViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let normalWindows = scenes.flatMap { $0.windows }.filter { $0.windowLevel == .normal && !$0.isHidden }
        // 优先返回 key window；浮动按钮 window 可能抢占 key 时，回退到任意 normal window
        let window = normalWindows.first(where: { $0.isKeyWindow }) ?? normalWindows.first
        return window?.rootViewController?.topViewController()
    }

    private static var documentsDirectory: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
    }

    private static func summarize(_ node: SemanticNode) -> String {
        var counter = [String: Int]()
        func walk(_ n: SemanticNode) {
            let role = n.role.snapshotName
            counter[role, default: 0] += 1
            for c in n.children { walk(c) }
        }
        walk(node)
        let sorted = counter.sorted { $0.value > $1.value }.prefix(6)
        let parts = sorted.map { "\($0.key)×\($0.value)" }
        return "角色分布：" + parts.joined(separator: " ")
    }

    private static func inferNativeBaselineId(from activityClassName: String) -> String {
        if activityClassName.contains("MessageHome") { return "ios_message_home" }
        if activityClassName.contains("Contacts") { return "ios_contacts" }
        if activityClassName.contains("Mail") { return "ios_mail" }
        if activityClassName.contains("Business") { return "ios_business" }
        return "ios_\(activityClassName.lowercased())"
    }
}

private extension UIViewController {
    func topViewController() -> UIViewController? {
        if let presented = presentedViewController {
            return presented.topViewController()
        }
        if let nav = self as? UINavigationController {
            return nav.visibleViewController?.topViewController()
        }
        if let tab = self as? UITabBarController, let selected = tab.selectedViewController {
            return selected.topViewController()
        }
        return self
    }

    func topMost() -> UIViewController {
        if let presented = presentedViewController {
            return presented.topMost()
        }
        if let nav = self as? UINavigationController {
            return nav.visibleViewController?.topMost() ?? self
        }
        return self
    }
}
