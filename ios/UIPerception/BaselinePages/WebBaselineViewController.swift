import UIKit
import WebKit

/// Web baseline 页面，加载一个本地 HTML 用于验证 web 抓取链路。
/// 对应 Android 的 WebBaselinePlaceholderActivity（实际版）。
final class WebBaselineViewController: UIViewController {
    private let webView: WKWebView = {
        // iOS 14+ WKWebView 默认启用 JavaScript，无需显式设置。
        let view = WKWebView(frame: .zero)
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()

    private let pageName: String

    init(pageName: String = "web_form") {
        self.pageName = pageName
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        view.addSubview(webView)
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        loadLocalPage()
    }

    private func loadLocalPage() {
        guard let url = Bundle.main.url(forResource: pageName, withExtension: "html") else {
            webView.loadHTMLString("<h1>找不到 \(pageName).html</h1>", baseURL: nil)
            return
        }
        webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
    }
}
