import UIKit

/// 浮动感知按钮，对应 Android `CaptureFloatingButton.java`。
/// 用独立 UIWindow（level = .alert + 1）承载，无需系统 overlay 权限。
/// 点击触发一次 capture 流程，并展示结果路径。
final class FloatingButton {

    static let shared = FloatingButton()

    private var window: UIWindow?
    private var button: UIButton?
    private var loadingLayer: CAShapeLayer?

    private init() {}

    func show(in scene: UIWindowScene) {
        guard window == nil else { return }

        let window = PassthroughWindow(windowScene: scene)
        window.windowLevel = .alert + 1
        window.backgroundColor = .clear
        window.isHidden = false

        let frame = CGRect(x: UIScreen.main.bounds.width - 88, y: 120, width: 64, height: 64)
        let button = UIButton(type: .custom)
        button.frame = frame
        button.backgroundColor = .systemBlue
        button.layer.cornerRadius = frame.width / 2
        button.layer.shadowColor = UIColor.black.cgColor
        button.layer.shadowRadius = 8
        button.layer.shadowOpacity = 0.3
        button.layer.shadowOffset = CGSize(width: 0, height: 4)
        button.setTitle("感知", for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 13, weight: .semibold)
        button.setTitleColor(.white, for: .normal)
        button.addTarget(self, action: #selector(handleTap), for: .touchUpInside)
        button.accessibilityIdentifier = "floating_capture_button"

        let root = UIViewController()
        root.view = UIView()
        root.view.backgroundColor = .clear
        root.view.addSubview(button)

        window.rootViewController = root
        // PassthroughWindow 的 hitTest 会处理 root.view 区域的事件透传，
        // 不需要禁用 root.view.isUserInteractionEnabled（那会把 button 也屏蔽掉）。
        window.makeKeyAndVisible()
        // 重新让 app 主窗口成为 key，避免影响输入
        scene.windows.first(where: { $0 !== window && !$0.isHidden })?.makeKey()

        self.window = window
        self.button = button
    }

    func hide() {
        window?.isHidden = true
        window = nil
        button = nil
        loadingLayer = nil
    }

    @objc private func handleTap() {
        guard let button else { return }
        // 不要在 capture 之前禁用 button.isUserInteractionEnabled，
        // 否则 dump 出来的 snapshot 会把按钮标成 [disabled]。
        startLoading(on: button)
        button.isUserInteractionEnabled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            self?.runCapture()
            self?.stopLoading()
        }
    }

    private func runCapture() {
        // CaptureCoordinator 自己负责 present 结果 alert；
        // 这里只触发，不重复展示。
        CaptureCoordinator.capture()
    }

    private func startLoading(on button: UIButton) {
        let layer = CAShapeLayer()
        let radius: CGFloat = 26
        let path = UIBezierPath(
            arcCenter: CGPoint(x: button.bounds.midX, y: button.bounds.midY),
            radius: radius,
            startAngle: 0,
            endAngle: .pi * 2,
            clockwise: true
        )
        layer.path = path.cgPath
        layer.fillColor = UIColor.clear.cgColor
        layer.strokeColor = UIColor.white.cgColor
        layer.lineWidth = 3
        layer.lineCap = .round
        layer.strokeStart = 0
        layer.strokeEnd = 0.3
        button.layer.addSublayer(layer)

        let anim = CABasicAnimation(keyPath: "transform.rotation")
        anim.fromValue = 0
        anim.toValue = 2 * Double.pi
        anim.duration = 0.8
        anim.repeatCount = .infinity
        layer.add(anim, forKey: "rotate")

        loadingLayer = layer
        button.isUserInteractionEnabled = false
    }

    private func stopLoading() {
        loadingLayer?.removeFromSuperlayer()
        loadingLayer = nil
        button?.isUserInteractionEnabled = true
    }
}

/// 让窗口内非按钮区域事件透传到下层。
private final class PassthroughWindow: UIWindow {
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        guard let view = super.hitTest(point, with: event) else { return nil }
        // 只有命中按钮或其子视图才拦截；其它事件透传
        return view === self || view === rootViewController?.view ? nil : view
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
}
