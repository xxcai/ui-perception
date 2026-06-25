# iOS Agent Bridge — 方案梳理与步骤拆解

> 状态：方案设计（pre-implementation）
> 对齐 Android：`perception-sdk/src/main/java/com/hh/uiperception/sdk/internal/` + `samples/agent-bridge/`
> 目标：让 LLM agent 通过 HTTP 调用 iOS app 内的操作（click / type / scroll / exec_js / capture …）

## 1. Android 现状回顾

完整架构见 [Explore 报告](#)，简版：

- **传输**：app 内嵌 HTTP server（port 9700，自写 ServerSocket），host 通过 `adb forward tcp:9700 tcp:9700` 映射到本机
- **副传输**：ContentProvider（`{appId}.perception-sdk.init`）——既用来 auto-init，又支持 `adb shell content call` 直发指令
- **操作清单**（15 个）：`ping` / `capture` / `capture-raw` / `debug-dom` / `click` / `long_press` / `swipe` / `type_text` / `check` / `uncheck` / `select_option` / `press_key` / `exec_js` / `open_uri`
- **核心数据结构**：`RefBoundsCache`——存 `ref → [l,t,r,b]` 屏幕坐标 + `webElementIdx`，click 时按坐标 dispatch `MotionEvent`
- **WebView 操作**：`findWebView(decorView)` → `evaluateJavascript(js)`，CountDownLatch 5s 同步等
- **Host 客户端**：`.opencode/plugins/phone-auto-test.ts`（OpenCode 插件）+ `scripts/test-web-ops.sh`（bash + curl）

## 2. iOS 与 Android 的关键差异

| 维度 | Android | iOS | 对 PoC 的影响 |
|---|---|---|---|
| 触摸事件模拟 | `dispatchTouchEvent(MotionEvent)` 公开 | **私有 API**，App Store 禁用 | 🔴 必须 ref→UIView→`sendActions`，不能按坐标点击 |
| 跨进程 IPC | ContentProvider 公开 | App 沙箱，**无对应** | 🟡 HTTP server 是唯一路径 |
| Host→设备 隧道 | `adb forward` | USB→`pymobiledevice3 tunnel` 或 LAN→WiFi 直连 | 🟢 PoC 走 WiFi，最简单 |
| 后台运行 | Foreground service 保活 | App 后台被挂起，HTTP server 死 | 🟢 PoC 仅前台，记录在限制里 |
| Ref 解析 | ref→bounds→坐标 | ref→**UIView**（强引用 map） | 🟡 iOS 抓取时需维护 ref→view 映射 |
| 系统 Back 键 | `press_key=back` | 无全局 back，per-app（pop/dismiss） | 🟢 翻译成 `navigationController.popViewController` |
| Resource-id | `R.id.xxx` | `accessibilityIdentifier` | 🟢 已有 |

**核心架构差异**：Android click 是**坐标驱动**（ref→bounds→dispatchEvent at x,y），iOS click 必须**对象驱动**（ref→UIView→invoke action）。这是 iOS 方案最大的设计变更。

## 3. iOS 方案设计

### 3.1 总体架构

```
┌──────────────┐    HTTP/JSON     ┌────────────────────────────────┐
│ Host Agent   │ ───────────────► │ iOS App (foreground)           │
│ (Mac/Cloud)  │                  │                                │
│              │  /capture /click │  ┌──────────────────────────┐  │
│ OpenCode TS  │ ◄─────────────── │  │ PerceptionHTTPServer     │  │
│ /curl/Python │  /exec_js ...    │  │ (Network.framework)      │  │
└──────────────┘                  │  └────────┬─────────────────┘  │
                                  │           │                     │
                                  │  ┌────────▼─────────────────┐  │
                                  │  │ OperationRouter          │  │
                                  │  │ ├─ ClickHandler          │  │
                                  │  │ ├─ TypeTextHandler       │  │
                                  │  │ ├─ ExecJsHandler         │  │
                                  │  │ ├─ ...                   │  │
                                  │  └────────┬─────────────────┘  │
                                  │           │                     │
                                  │  ┌────────▼─────────────────┐  │
                                  │  │ RefRegistry              │  │
                                  │  │ [ref: weak UIView]       │  │
                                  │  │ [ref: webElementIdx]     │  │
                                  │  │ [ref: WKWebView]         │  │
                                  │  └──────────────────────────┘  │
                                  └────────────────────────────────┘
```

### 3.2 传输层选型

**选定**：`Network.framework`（NWListener），系统内置，无第三方依赖。

| 候选 | 优点 | 缺点 | 决定 |
|---|---|---|---|
| Network.framework | 内置、低层但够用、Swift | API 偏底层 | ✅ 选 |
| SwiftNIO | 高性能、成熟 | SPM 依赖、对 PoC 过重 | ❌ |
| GCDWebServer / Telegraph | API 友好 | 第三方依赖 | ❌（PoC 不引入） |

**端口**：`9700`（与 Android 对齐，host 客户端零改动）

**Host→设备链路**（按优先级）：

1. **WiFi/LAN 直连**（PoC 推荐）——Mac 与 iPhone 同子网，agent 直接访问 `http://<iPhone-IP>:9700`。需在 Info.plist 配 `NSLocalNetworkUsageDescription` + Bonjour 服务（可选）。
2. **USB 隧道**（备选）——`pymobiledevice3 tunnel start-qcu ... && pymobiledevice3 tunnel start-tunnel` + 端口转发。需在 Mac 装 pymobiledevice3。

PoC 阶段走方案 1。文档里写清两种方式 + 各自限制。

### 3.3 Auto-init（取代 ContentProvider）

iOS 不需要 ContentProvider 那种 trick。`AppDelegate.application(_:didFinishLaunchingWithOptions:)` 里直接 `PerceptionSDK.startHTTPServer()`。

无需 Application 子类化，无需 manifest 改动（iOS 没有 manifest）。

### 3.4 RefRegistry（替代 RefBoundsCache）

iOS 用 **ref → UIView** 弱引用 map，而不是 ref → bounds。设计：

```swift
final class RefRegistry {
    static let shared = RefRegistry()
    private(set) var refToView: [String: WeakRef<UIView>] = [:]
    private(set) var refToWebElementIdx: [String: Int] = [:]
    private(set) var refToWebView: [String: WeakRef<WKWebView>] = [:]  // web ref → 所属 WKWebView

    func update(from dump: UIKitViewNode, semanticRoot: SemanticNode) { ... }
    func updateWeb(json: String, webView: WKWebView) { ... }

    func view(for ref: String) -> UIView?
    func webView(for ref: String) -> (WKWebView, Int)?
}
```

更新时机：`CaptureCoordinator.capture()` 成功后，把 dump 阶段拿到的 UIView 引用和 assign 完的 ref 一并塞进 RefRegistry。

**难点**：现有的 `UIKitViewNode` 是 struct（值类型），抓取完就丢掉了 view 引用。需要：
- 要么改成 class 持有 weak view 引用
- 要么在 dumper 阶段额外输出 `[tag: UIView]` 或 `[accessibilityIdentifier + path: UIView]`

推荐方案：**在 UIKitHierarchyDumper 阶段，把每个 view 的 `tag` 设成一个唯一 token**（或用 `NSValue`/ObjectIdentifier），dump 完后建立 `[token: weak UIView]` + `[ref: token]` 两级映射。参考 Android `__pr_idx` 的做法。

### 3.5 操作清单（iOS 实现策略）

完整对齐 Android 15 个操作，标 iOS 实现难度：

| 操作 | iOS 实现 | 难度 | 备注 |
|---|---|---|---|
| `ping` | 直接返回 | 🟢 | - |
| `capture` | 复用 CaptureCoordinator.capture() | 🟢 | 已有 |
| `capture-raw` | 复用 dump 输出 | 🟢 | - |
| `debug-dom` | 复用 WebCapture 输出 | 🟢 | - |
| `click` (native) | ref→UIView → `UIControl.sendActions(.touchUpInside)` 或 `gestureRecognizers.first?.delegate` 触发 | 🟡 | UIControl 子类有 sendActions；普通 UIView 需要找最近的可点击祖先 |
| `click` (web) | ref→WKWebView + prIdx → 注入 `document.querySelector('[data-pr-idx="X"]').click()` | 🟡 | 需要在 dom-serializer.js 里给元素加 `data-pr-idx` 属性 |
| `long_press` | 类似 click，触发 `.touchDown` + delay + `.touchUp`；或调 `UILongPressGestureRecognizer` 的 state | 🟡 | - |
| `swipe` | UIScrollView → `setContentOffset(animated:true)`；非 scroll 用 transition animation | 🟡 | 与 Android 的"按方向滑动"语义不完全等价 |
| `type_text` | UITextField/UITextView → `.text = ...` 或 `.insertText(...)` | 🟢 | - |
| `check` / `uncheck` | UISwitch → `.setOn(animated:)` + send actions | 🟢 | - |
| `select_option` | UIPickerView → `selectRow(_, inComponent:, animated:)`；UISegmentedControl → `.selectedSegmentIndex = ...` | 🟡 | 多种 picker 控件需分别处理 |
| `press_key=back` | `UINavigationController.popViewController` 或 `dismiss` | 🟢 | iOS 无系统 back，per-app |
| `exec_js` | 复用 WebViewFinder + evaluateJavaScript + RunLoop pump（已踩过的坑见 [[ios_poc_known_issues]]） | 🟢 | 已有 |
| `open_uri` | `UIApplication.shared.open(URL)` | 🟢 | - |

### 3.6 工程结构

```
ios/UIPerception/
├── App/                           # 已有
├── BaselinePages/                 # 已有
├── Capture/                       # 已有
└── AgentBridge/                   # 🆕 新增
    ├── PerceptionHTTPServer.swift # NWListener 包装 + 请求路由
    ├── OperationRouter.swift      # path → handler 派发
    ├── RefRegistry.swift          # ref ↔ UIView/WKWebView 映射
    ├── WeakRef.swift              # 弱引用包装（防止 UIView 循环引用）
    ├── Handlers/
    │   ├── PingHandler.swift
    │   ├── CaptureHandler.swift
    │   ├── ClickHandler.swift
    │   ├── TypeTextHandler.swift
    │   ├── SwipeHandler.swift
    │   ├── SelectOptionHandler.swift
    │   ├── PressKeyHandler.swift
    │   ├── ExecJsHandler.swift
    │   └── OpenUriHandler.swift
    └── JSON/                      # 简单的 JSON 编解码（用 JSONSerialization）
```

AppDelegate 启动时调 `PerceptionHTTPServer.shared.start(port: 9700)`，停止时 `stop()`。

### 3.7 Sample / Test 基础页

复用已有的「Web 表单页」+「消息首页」，再加：

- **AgentTestViewController**：一个综合页，含 button/switch/textfield/picker/webview，让 agent 跑端到端回归
- 不需要 NonFullscreenTest（iOS 没有 Android 那种 Dialog-themed 多 window offset 问题）

### 3.8 Host 客户端复用

`.opencode/plugins/phone-auto-test.ts` 已经走 HTTP，**只需把 BASE_URL 从 `localhost:9700` 改成 `<iPhone-LAN-IP>:9700`** 就能驱动 iOS。adb forward 那段跳过。

scripts/test-web-ops.sh 同理。

## 4. 步骤拆解（按里程碑）

### M1 — HTTP server + ping 链路通（1-2 天）

- 实现 `PerceptionHTTPServer`（NWListener + 简单 JSON）
- 路由 `/ping` → 返回 `{version: "1.0.0-ios"}`
- AppDelegate 启动时拉起 server
- Info.plist 加 `NSLocalNetworkUsageDescription` + `NSAppTransportSecurity.allow ArbitraryLoads=true`（PoC 阶段，正式版收紧）
- **验收**：Mac 上 `curl http://<iPhone-IP>:9700/ping` 返回 version

### M2 — Capture 走 HTTP（半天）

- 实现 `CaptureHandler`，调 `CaptureCoordinator.capture()`，把 snapshot YAML 包到响应里
- **验收**：`curl http://<iPhone-IP>:9700/capture` 返回完整 YAML

### M3 — RefRegistry 基础设施（1-2 天）

- 改 `UIKitViewNode` 为 class + 持有 weak view 引用
- 改 `UIKitHierarchyDumper`，输出时给每个 view 设 `__pr_token`（hash of ObjectIdentifier，或用 tag 字段）
- 改 `RefAssigner`（或新增 `RefTokenBinder`）输出 `[ref: token]`
- 实现 `RefRegistry.shared` 单例
- CaptureHandler 完成后调 `RefRegistry.shared.update(...)`
- **验收**：抓取后能在 lldb 里看到 `RefRegistry.shared.view(for: "n3")` 返回正确 UIView

### M4 — Native 操作（click / type_text / check / swipe）（2-3 天）

- 实现 `ClickHandler`：ref→UIView，根据类型派发
  - `UIControl`：`sendActions(for: .touchUpInside)`
  - 有 `UITapGestureRecognizer`：手动触发
  - 有 `accessibilityTraits.button`：`accessibilityActivate()`
  - fallback：沿 superview 链找最近可点击祖先
- 实现 `TypeTextHandler`：`UITextField.text = ...` / `UITextView.insertText(...)`
- 实现 `CheckHandler` / `UncheckHandler`：`UISwitch.setOn(animated:)` + `sendActions`
- 实现 `SwipeHandler`：UIScrollView → `setContentOffset`（按方向算目标 offset）
- **验收**：用 AgentTestViewController 跑 click/type/check/swipe，每个操作都有可见响应

### M5 — Web 操作（exec_js / click web ref / select_option）（1-2 天）

- 实现 `ExecJsHandler`：复用 WebCapture 的 RunLoop pump 模式
- 改 `dom-serializer.js`（与 Android 共享）给每个序列化节点加 `data-pr-idx` 属性
- 实现 web ref → click：注入 `(document.querySelector('[data-pr-idx=\"X\"]')||{}).click?.()` JS
- 实现 `SelectOptionHandler`：web 用 JS dispatch change；native UIPickerView/UISegmentedControl 分别处理
- **验收**：在 Web 表单页点击「提交」按钮、下拉选「产品部」、文本框输入文字都成功

### M6 — 剩余操作 + 收尾（1 天）

- `long_press` / `press_key=back` / `open_uri` / `capture-raw` / `debug-dom`
- 错误响应统一 envelope：`{status: "error", error: "..."}`
- **验收**：用 `scripts/test-web-ops.sh`（已存在）改 BASE_URL 跑一遍全操作

### M7 — Host 客户端打通（半天）

- `.opencode/plugins/phone-auto-test.ts` 加 iOS 分支：检测 `IDEVICE_<UDID>` 环境变量或 LAN IP，跳过 `adb forward`
- 或直接复制一份 `phone-auto-test-ios.ts`
- **验收**：OpenCode 里说「点击 Web 表单页的提交按钮」→ 实际触发

## 5. 风险与开放问题

| 风险 | 等级 | 缓解 |
|---|---|---|
| App 后台时 HTTP server 被挂起 | 🟡 | PoC 仅前台运行；正式版用 Background Mode `audio` 或 `location` 保活（但有审核风险） |
| `sendActions` 不能覆盖所有点击场景（如自定义手势） | 🟡 | 列出 fallback 链；记录哪些 view 类型不可操作 |
| 触摸事件 vs `sendActions` 语义差异（不触发底层手势识别器） | 🟡 | 与 Android MotionEvent 行为不完全一致，文档里标注 |
| dom-serializer.js 给节点加 data-pr-idx 会改 HTML | 🟢 | 加在 capture 期间，capture 后清除；或用 WeakMap |
| SwiftUI HostingView 内部 view 拿不到 ref | 🟡 | PoC 仅 UIKit，SwiftUI 不在范围 |
| NSLocalNetworkUsageDescription 弹窗打扰用户 | 🟢 | PoC 可接受，正式版考虑 Bonjour 发现 |
| Web 坐标转屏坐标（webScale / webViewOffset） | 🟢 | iOS 不需要坐标点击，问题自动消失 |

## 6. 不在 PoC 范围

- 跨 App 操作（iOS 沙箱禁止，需要越狱或 MDM 受管设备）
- XCTest UI 自动化路径（需要插桩 + 测试 host，不是 in-app）
- SwiftUI HostingView 内部点击
- Background mode / 后台保活
- App Store 合规（HTTP server / 私有 API 会被拒）
- 多设备管理 / 远程云手机
- 鸿蒙对应实现（iOS PoC 跑通后再启动）

## 7. 验证标准（PoC 完成判定）

端到端跑通 4 个典型 agent 任务：

1. **Read**：agent 说「这个页面有什么」→ capture 返回 YAML → agent 用 LLM 总结内容
2. **Fill form**：agent 说「在用户名输入框填 '梁晓舟'」→ type_text(n=...) → 文本框出现文字
3. **Click button**：agent 说「点击提交按钮」→ click(n=...) → 按钮触发提交逻辑
4. **Web interaction**：agent 说「选择部门为产品部」→ select_option(w=..., value=...) → 下拉框选中

四个全过 = PoC 成功，可以推进 P0 工程项。

## 8. 关联

- Android 实现：`perception-sdk/src/main/java/com/hh/uiperception/sdk/internal/`
- 已踩坑（RunLoop pump）：[[ios_poc_known_issues]] memory
- Web 输出属性缺失：[Issue #001](../issues/001-web-dom-serializer-missing-props.md)
- 跨端 PoC 整体：[ios-port-research.md](./ios-port-research.md)
- Host 客户端：`.opencode/plugins/phone-auto-test.ts`、`scripts/test-web-ops.sh`
