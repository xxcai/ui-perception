import UIKit
import WebKit

/// 从 UIView 树中找到当前的 WKWebView。
/// 对应 Android `WebViewFinder.java`（简化版，PoC 只找第一个 WKWebView）。
enum WebViewFinder {

    static func find(in view: UIView) -> WKWebView? {
        if let web = view as? WKWebView { return web }
        for sub in view.subviews {
            if let found = find(in: sub) { return found }
        }
        return nil
    }

    /// 找当前 active scene 的 key window 中的 WKWebView。
    static func findCurrent() -> WKWebView? {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive })
            ?? UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first,
              let window = scene.windows.first(where: { $0.windowLevel == .normal && !$0.isHidden })
                    ?? scene.windows.first(where: { $0.windowLevel == .normal }),
              let root = window.rootViewController
        else { return nil }
        return find(in: root.view)
    }
}

/// Web capture 入口。对应 Android `WebPerceptionPlugin.capture + transform`。
///
/// 流程：找 WKWebView → 注入 dom-serializer.js → JSON → WebJsonParser → TreeNormalizer
///     → RefAssigner("w", webMode: true) → SnapshotRenderer
enum WebCapture {

    struct CaptureResult {
        let summary: String
        let snapshot: String
        let outputURL: URL?
    }

    private enum EvalResult {
        case ok(String)
        case error(String)
        case timeout
    }

    /// 同步调用 evaluateJavaScript。
    /// ⚠️ 不能用 DispatchSemaphore.wait 阻塞主线程：completion handler 在主队列派发，
    /// 主线程被阻塞 → completion 永远跑不到 → 死锁到 timeout。
    /// 用 RunLoop pump 让主队列任务能继续派发。
    private static func evalSync(webView: WKWebView, js: String, timeout: TimeInterval = 10.0) -> EvalResult {
        var done = false
        var rawResult: String?
        var evalError: Error?

        webView.evaluateJavaScript(js) { result, error in
            if let error { evalError = error }
            else if let str = result as? String { rawResult = str }
            else if let data = try? JSONSerialization.data(withJSONObject: result as Any),
                    let str = String(data: data, encoding: .utf8) {
                rawResult = str
            }
            done = true
        }

        let deadline = Date().addingTimeInterval(timeout)
        while !done && Date() < deadline {
            RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.05))
        }

        if !done { return .timeout }
        if let evalError { return .error(String(describing: evalError)) }
        return .ok(rawResult ?? "")
    }

    /// 同步入口（在主线程调用）。会阻塞当前线程 10s 等 JS 执行完。
    /// 因为 evaluateJavaScript 是异步的，这里用 semaphore 等待。
    static func capture() -> CaptureResult {
        guard let webView = WebViewFinder.findCurrent() else {
            return CaptureResult(
                summary: "未找到 WKWebView，请在 Web baseline 页面重试",
                snapshot: "",
                outputURL: nil
            )
        }

        guard let js = loadDomSerializerJS() else {
            return CaptureResult(summary: "无法加载 dom-serializer.js", snapshot: "", outputURL: nil)
        }

        let result = evalSync(webView: webView, js: js)
        let json: String
        switch result {
        case .timeout:
            return CaptureResult(summary: "JS 注入超时（10s）", snapshot: "", outputURL: nil)
        case .error(let e):
            return CaptureResult(summary: "JS 执行失败：\(e)", snapshot: "", outputURL: nil)
        case .ok(let s):
            json = s
        }
        if json.isEmpty {
            return CaptureResult(summary: "JS 返回空结果", snapshot: "", outputURL: nil)
        }

        guard let tree = WebJsonParser.parse(json) else {
            return CaptureResult(summary: "JSON 解析失败", snapshot: "", outputURL: nil)
        }
        let normalized = TreeNormalizer.normalize(tree)
        guard let withRefs = RefAssigner.assign(normalized, prefix: "w", webMode: true) else {
            return CaptureResult(summary: "归一化失败", snapshot: "", outputURL: nil)
        }
        let snapshot = SnapshotRenderer.render(withRefs)

        let baseline = inferBaselineId(from: webView.url?.path ?? "")
        let timestamp = Int(Date().timeIntervalSince1970 * 1000)
        let runDir = documentsDirectory
            .appendingPathComponent("captures")
            .appendingPathComponent(baseline)
            .appendingPathComponent("runs")
            .appendingPathComponent(String(timestamp))
            .appendingPathComponent("web")
        let rawDir = runDir.appendingPathComponent("raw")
        let transformedDir = runDir.appendingPathComponent("transformed")
        try? FileManager.default.createDirectory(at: rawDir, withIntermediateDirectories: true)
        try? FileManager.default.createDirectory(at: transformedDir, withIntermediateDirectories: true)

        let rawPath = rawDir.appendingPathComponent("dom_dump_\(timestamp).json")
        let transformedPath = transformedDir.appendingPathComponent("llm_input_\(timestamp).yml")
        try? json.data(using: .utf8)?.write(to: rawPath)
        try? snapshot.data(using: .utf8)?.write(to: transformedPath)

        let summary = """
        \(summarize(withRefs))
        URL：\(webView.url?.absoluteString ?? "(未知)")
        文件：\(transformedPath.lastPathComponent)
        """
        return CaptureResult(summary: summary, snapshot: snapshot, outputURL: transformedPath)
    }

    /// 从 app bundle 加载 dom-serializer.js。
    private static var cachedJS: String?
    private static func loadDomSerializerJS() -> String? {
        if let cachedJS { return cachedJS }
        guard let url = Bundle.main.url(forResource: "dom-serializer", withExtension: "js"),
              let data = try? Data(contentsOf: url),
              let str = String(data: data, encoding: .utf8)
        else { return nil }
        cachedJS = str
        return str
    }

    private static var documentsDirectory: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
    }

    private static func summarize(_ node: SemanticNode) -> String {
        var counter = [String: Int]()
        func walk(_ n: SemanticNode) {
            counter[n.role.snapshotName, default: 0] += 1
            for c in n.children { walk(c) }
        }
        walk(node)
        let sorted = counter.sorted { $0.value > $1.value }.prefix(6)
        let parts = sorted.map { "\($0.key)×\($0.value)" }
        return "角色分布：" + parts.joined(separator: " ")
    }

    private static func inferBaselineId(from path: String) -> String {
        if path.contains("web_form") { return "ios_web_form" }
        if path.contains("web_chat") { return "ios_web_chat" }
        return "ios_web_\(path.hashValue & 0xFFFFFF)"
    }
}
