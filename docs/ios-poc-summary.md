# iOS UIKit 抓取 PoC 总结

> 范围：评估 UI Perception 能力在 iOS（UIKit）上的可复刻性，作为跨端架构的第二个验证端
> 状态：✅ Native + Web 抓取链路跑通；🟡 Agent Bridge 方案设计完成，未实施
> 完成时间：2026-06-25（Native + Web PoC）

## 1. 要回答的问题

| 问题 | 结论 |
|---|---|
| iOS 有没有等价于 Android `native_xml` 的 UI 树数据源？ | ✅ UIView 树递归遍历（`UIKitHierarchyDumper`） |
| 能否拿到跟 Android 同结构的 semantic snapshot？ | ✅ 1:1 对齐，95 行 YAML（消息首页） |
| 跨端算法能否复用？ | ✅ Bounds/RefAssigner/SnapshotRenderer 等核心算法从 Java 逐行移植到 Swift |
| Web H5 能否抓？ | ✅ WKWebView + `evaluateJavaScript` + dom-serializer.js（与 Android 共享） |
| Agent Bridge 在 iOS 上能否成立？ | 🟡 方案设计完成；触摸事件合成被私有 API 限制，需对象驱动改造 |

## 2. 两条抓取路径

### 2.1 Native (UIKit)

**数据源**：`UIKitHierarchyDumper.dump()`，从 key window（过滤 `windowLevel == .normal`）开始递归遍历 `view.subviews`

**关键字段映射**：

| SemanticNode 字段 | iOS 来源 |
|---|---|
| role | UIKit class-name 表（`iOSRoleResolver`，覆盖 ~15 常用类） |
| name | `accessibilityLabel` ‖ 子 UILabel 文本聚合 ‖ `accessibilityIdentifier` |
| bounds | `view.frame` 转 int `[l, t, r, b]` |
| ref | role 白名单 + valid bounds → `n1, n2, ...`（与 Android 共享） |
| clickable | `view is UIControl` ‖ `gestureRecognizers != nil`（**不能**用 `accessibilityTraits`，会跨层透传） |
| enabled | `view.isUserInteractionEnabled`（仅对交互角色生效） |
| selected/checked | `UISwitch.isOn` / `UISegmentedControl.selectedSegmentIndex` |

**clickable 判定关键约束**：

```swift
// ❌ 错误做法：accessibilityTraits 会从父 view 跨层透传到子 UILabel
if view.accessibilityTraits.contains(.button) { ... }

// ✅ 正确做法：只信任类型 + gesture recognizer
let clickable = (view is UIControl) || !(view.gestureRecognizers?.isEmpty ?? true)
```

### 2.2 Web (WKWebView)

**数据源**：`WKWebView.evaluateJavaScript(domSerializerJs)` → 拿到 dom-serializer.js 同步返回的 JSON → `WebJsonParser` → `TreeNormalizer` → `RefAssigner("w", webMode=true)` → `SnapshotRenderer`

**关键资产**：`ios/UIPerception/Capture/dom-serializer.js`（718 行 IIFE，MD5 与 Android / 鸿蒙完全一致：`76793967ba7c96ff4aae193ae344d677`）

**异步处理**（与 Android 关键差异）：

```swift
// ❌ 错误做法：DispatchSemaphore 在主线程死锁
DispatchSemaphore.wait()  // evaluateJavaScript completion 永不回调

// ✅ 正确做法：RunLoop pump
while Date().timeIntervalSince(start) < timeout {
    RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.05))
    if completed { break }
}
```

## 3. Agent Bridge（设计完成，未实施）

### 3.1 与 Android 的关键差异

| 维度 | Android | iOS | 影响 |
|---|---|---|---|
| 触摸事件模拟 | `dispatchTouchEvent(MotionEvent)` 公开 | **私有 API**，App Store 禁用 | 🔴 必须 ref→UIView→`sendActions`，不能按坐标点击 |
| 跨进程 IPC | ContentProvider 公开 | App 沙箱，**无对应** | 🟡 HTTP server 是唯一路径 |
| Host→设备隧道 | `adb forward` | USB→`pymobiledevice3 tunnel` 或 LAN→WiFi 直连 | 🟢 PoC 走 WiFi，最简单 |
| 后台运行 | Foreground service 保活 | App 后台被挂起，HTTP server 死 | 🟢 PoC 仅前台，记录在限制里 |
| Ref 解析 | ref→bounds→坐标 | ref→**UIView**（弱引用 map） | 🟡 iOS 抓取时需维护 ref→view 映射 |
| Resource-id | `R.id.xxx` | `accessibilityIdentifier` | 🟢 已有 |
| 系统 Back 键 | `press_key=back` | 无全局 back，per-app（pop/dismiss） | 🟢 翻译成 `navigationController.popViewController` |

**核心架构差异**：Android click 是**坐标驱动**（ref→bounds→dispatchEvent at x,y），iOS click 必须**对象驱动**（ref→UIView→invoke action）。这是 iOS 方案最大的设计变更。

### 3.2 传输层

- **选定**：`Network.framework`（NWListener），系统内置，无第三方依赖
- **端口**：`9700`（与 Android 对齐，host 客户端零改动）
- **Host→设备链路**（按优先级）：
  1. **WiFi/LAN 直连**（PoC 推荐）：Mac 与 iPhone 同子网，agent 直接访问 `http://<iPhone-IP>:9700`。需在 Info.plist 配 `NSLocalNetworkUsageDescription` + Bonjour 服务（可选）
  2. **USB 隧道**（备选）：`pymobiledevice3 tunnel start-qcu ... && pymobiledevice3 tunnel start-tunnel` + 端口转发。需在 Mac 装 pymobiledevice3
  3. ❌ pymobiledevice3 tunnel 不可用：只能到 Apple 私有 CoreDevice 服务，到不了 App 端口；私有 entitlement 会被 App Store 拒审

### 3.3 Auto-init

`AppDelegate.application(_:didFinishLaunchingWithOptions:)` 里直接 `PerceptionSDK.startHTTPServer()`，无需 Application 子类化，无需 manifest 改动（iOS 没有 manifest）。

### 3.4 RefRegistry（替代 RefBoundsCache）

```swift
final class RefRegistry {
    static let shared = RefRegistry()
    private(set) var refToView: [String: WeakRef<UIView>] = [:]
    private(set) var refToWebElementIdx: [String: Int] = [:]
    private(set) var refToWebView: [String: WeakRef<WKWebView>] = [:]
}
```

**难点**：现有 `UIKitViewNode` 是 struct（值类型），抓取完就丢掉了 view 引用。需要在 `UIKitHierarchyDumper` 阶段给每个 view 设 `__pr_token`（hash of ObjectIdentifier 或 tag 字段），建立 `[token: weak UIView]` + `[ref: token]` 两级映射。

### 3.5 操作清单（15 个，跨端对齐）

| 操作 | iOS 实现 | 难度 |
|---|---|---|
| `ping` | 直接返回 | 🟢 |
| `capture` | 复用 CaptureCoordinator.capture() | 🟢 |
| `capture-raw` / `debug-dom` | 复用 dump / WebCapture 输出 | 🟢 |
| `click` (native) | ref→UIView → `UIControl.sendActions(.touchUpInside)`；普通 UIView 找最近可点击祖先 | 🟡 |
| `click` (web) | ref→WKWebView + prIdx → 注入 `document.querySelector('[data-pr-idx="X"]').click()` | 🟡 |
| `long_press` | `.touchDown` + delay + `.touchUp`；或调 `UILongPressGestureRecognizer.state` | 🟡 |
| `swipe` | UIScrollView → `setContentOffset(animated:true)`；非 scroll 用 transition animation | 🟡 |
| `type_text` | `UITextField.text = ...` / `UITextView.insertText(...)` | 🟢 |
| `check` / `uncheck` | `UISwitch.setOn(animated:)` + send actions | 🟢 |
| `select_option` | UIPickerView → `selectRow(_, inComponent:, animated:)`；UISegmentedControl → `.selectedSegmentIndex = ...` | 🟡 |
| `press_key=back` | `UINavigationController.popViewController` 或 `dismiss` | 🟢 |
| `exec_js` | 复用 WebCapture RunLoop pump 模式 | 🟢 |
| `open_uri` | `UIApplication.shared.open(URL)` | 🟢 |

## 4. 工程结构

```
ios/
├── project.yml                                # XcodeGen 配置（声明式生成 xcodeproj）
├── UIPerception.xcodeproj/                    # 生成产物（gitignore）
└── UIPerception/
    ├── App/                                   # app 入口
    │   ├── AppDelegate.swift
    │   ├── SceneDelegate.swift                # iOS 14+ UISceneSession
    │   ├── RootViewController.swift           # baseline 页面列表
    │   └── Info.plist
    ├── BaselinePages/
    │   └── MessageHomeViewController.swift    # 复刻 Android 消息首页
    └── Capture/                               # 算法层 + iOS 抓取层（15 文件 ~1100 LOC）
        ├── SemanticRole.swift                 # ← SemanticRole.java 逐行移植
        ├── Bounds.swift                       # ← Bounds.java
        ├── RoleDecision.swift                 # ← RoleDecision.java
        ├── SemanticNode.swift                 # ← SemanticNode.java
        ├── SnapshotRenderer.swift             # ← SnapshotRenderer.java
        ├── RefAssigner.swift                  # ← RefAssigner.java
        ├── UIKitViewNode.swift                # iOS 抓取中间表示
        ├── UIKitHierarchyDumper.swift         # ← ViewHierarchyDumper.java
        ├── iOSRoleResolver.swift              # ← NativeRoleResolver.java (UIKit class 表)
        ├── SemanticTreeBuilder.swift          # ← NativeSemanticTreeBuilder.java
        ├── WebCapture.swift                   # WKWebView + dom-serializer.js
        ├── WebJsonParser.swift                # ← WebJsonParser.java
        ├── dom-serializer.js                  # ⭐ 三端共享（MD5 一致）
        ├── FloatingButton.swift               # 浮动感知按钮
        └── CaptureCoordinator.swift           # capture 入口
```

**代码量**：15 个 Swift 文件 ~1100 LOC。其中 6 个是从 Java 逐行移植（算法层），4 个是 iOS 特有重写（抓取层 + UI）。

**AgentBridge/** 目录（🆕 待新增）：`PerceptionHTTPServer.swift` / `OperationRouter.swift` / `RefRegistry.swift` / `WeakRef.swift` / `Handlers/*.swift`

## 5. 验证结果

### Native（消息首页）

```yaml
- screen:
  - generic:
    - screen:
      - button [ref=n1] [bounds=0,0,402,874]:        # root VC view
        - generic:
          - generic:
            - generic:
              - input "搜索" [ref=n2]:               # UITextField ✓
              - button "新增" [ref=n5]:               # UIButton ✓
              - button "客服" [ref=n7]:               # UIButton ✓
              - list [scrollable] [ref=n9]:           # UITableView ✓
                - text "韩辰，在线":                   # UITableViewCell
                - text "李妍，请假":
                - ...
    - toolbar: - text "消息首页"                       # UINavigationBar ✓
```

95 行 YAML，跨端结构对齐。

### Web（web_form.html）

35 行 YAML，含 heading/textbox/combobox/button/list/listitem 全角色，UTF-8 中文完整保留。

### 跨端一致性

iOS 输出与 Android 1:1 对齐（除 bounds 数值与 iOS 缺失 resource-id）。

| 维度 | Android | iOS | 评价 |
|---|---|---|---|
| 列表容器 role | `scroll [scrollable]` | `list [scrollable]` | iOS 用 UITableView，源 UI 差异 |
| 列表项 role | `button`（推断可点） | `text` | iOS 应该是 `listitem`，role 表需补 UITableViewCell 父链判定 |
| 列表项 name | 各子 TextView 单独输出 | `accessibilityLabel` 合并为一句 | ✅ iOS 更聚合，更适合 LLM |
| 搜索框 role | `button`（被误判） | `input` | ✅ iOS 更准确 |
| 输出格式 | 自定义 YAML serializer | 同格式，同 serializer 算法 | ✅ 跨端完全等价 |

## 6. 跨端架构对齐

```
                ┌─────────────────────────────────────┐
                │  Shared algorithm layer             │
                │  (Java → Swift, 1:1 port)           │
                │                                     │
                │  Bounds / SemanticRole / Node       │
                │  RefAssigner / SnapshotRenderer     │
                │  WebJsonParser / TreeNormalizer     │
                │  dom-serializer.js (718 行 IIFE)    │
                └────────┬────────────────────────────┘
                         │
                ┌────────▼────────────┐
                │ iOS side            │
                │ UIKitHierarchyDumper│
                │ iOSRoleResolver     │
                │ WKWebView +         │
                │ evaluateJavaScript  │
                │ + RunLoop pump      │
                └─────────────────────┘
```

**对齐情况**：
- ✅ Bounds: 三端 `[l, t, r, b]` int 格式一致
- ✅ RefAssigner: native prefix="n" + web prefix="w" 一致
- ✅ SnapshotRenderer: YAML 缩进/状态标记/ref 语法一致
- ✅ dom-serializer.js: iOS / Android / 鸿蒙 MD5 完全一致
- ⚠️ Role 映射表: 来源不同（Android View class / iOS UIKit class / ArkUI `$type`）

## 7. 踩坑记录

| 坑 | 表现 | 根因 | 修复 |
|---|---|---|---|
| 浮动按钮 window 抢占 keyWindow | 抓到了浮动按钮 window（仅 6 节点） | `scene.windows.first(where: { $0.isKeyWindow })` 优先命中 alert level window | 显式过滤 `windowLevel == .normal` |
| accessibilityTraits 跨层透传 | `button "感知"` 套 `button "感知"`（父子都被识别为 button） | UIButton 内部 UILabel 继承父控件 traits | clickable 判定只信任 `view is UIControl` 和 `gestureRecognizers` |
| Swift Optional 被 `String(describing:)` 字符串化 | baselineId 变成 `ios_optional<uiviewcontroller>` | `var current: UIViewController?` 包成 Optional 后取类型名 | 解包后声明非 Optional 类型 |
| loading 期间禁用按钮污染 capture 状态 | 浮动按钮被标 `[disabled]` | capture 在 loading 中触发，`isUserInteractionEnabled = false` 状态被记录 | capture 期间强制还原 `true` |
| `evaluateJavaScript` + DispatchSemaphore 主线程死锁 | completion 永不回调 | 主线程 wait 阻塞了 main queue | RunLoop.current.run pump 替代 |
| `loadHTMLString` 中文乱码 | UTF-8 中文按 ISO-8859-1 重编码 | API 内部编码处理 | 用 `loadFileURL(_:allowingReadAccessTo:)` |
| iOS 26.3 simulator 字体回归 | 中文字符渲染成 `?` | 模拟器 runtime bug | 真机验证（模拟器仅做功能联调） |

完整踩坑清单见 memory：`ios_poc_known_issues.md`。

## 8. 工程限制与决策

| 限制 | iOS 应对 |
|---|---|
| 触摸事件合成是私有 API | 改对象驱动：ref→UIView→`sendActions` |
| App 沙箱禁止跨 App 操作 | PoC 仅驱动自身 App；跨 App 走 WebDriverAgent 独立路径（评估后已否决） |
| `evaluateJavaScript` 异步阻塞 | RunLoop pump 替代 semaphore |
| App 后台被挂起 | PoC 仅前台；正式版用 Background Mode（有审核风险） |
| 无 ContentProvider 等价物 | HTTP server 是唯一通道；砍掉 Android 的副通道 |
| `pymobiledevice3 tunnel` 不可用 | PoC 走 WiFi/LAN |

## 9. WebDriverAgent 评估（结论：不采用）

| 维度 | WDA 方案 | in-app 方案 |
|---|---|---|
| 跨 App 操作 | ✅ 任意 App | ❌ 仅自身 |
| Capture 数据源 | ⚠️ Accessibility 树（被过滤） | ✅ UIView 树（原始层级） |
| ref 一致性 | ❌ 与现有 `n/w` ref 系统冲突 | ✅ 复用 |
| iOS 升级稳定性 | 🔴 经常断 | 🟢 App 内代码不受影响 |
| 部署复杂度 | 🔴 装 Appium + build WDA + 签名 | 🟢 App 启动即可 |
| 速度 | 🟡 IPC ~100-500ms | 🟢 <10ms |

**致命冲突**：WDA 只看 Accessibility 树，与 UIView 树 capture 不兼容。混合方案也不可行（两边 ref 语义不同）。WDA 适合「LLM agent 操作任意 App」的独立项目，不适合作为 SDK 内 capture+ops 闭环的组件。

## 10. 风险与开放问题

| 风险 | 等级 | 缓解 |
|---|---|---|
| App 后台时 HTTP server 被挂起 | 🟡 | PoC 仅前台；正式版用 Background Mode `audio` / `location` 保活（但有审核风险） |
| `sendActions` 不能覆盖所有点击场景（如自定义手势） | 🟡 | 列出 fallback 链；记录哪些 view 类型不可操作 |
| 触摸事件 vs `sendActions` 语义差异（不触发底层手势识别器） | 🟡 | 与 Android MotionEvent 行为不完全一致，文档里标注 |
| dom-serializer.js 给节点加 `data-pr-idx` 会改 HTML | 🟢 | 加在 capture 期间，capture 后清除；或用 WeakMap |
| SwiftUI HostingView 内部 view 拿不到 ref | 🟡 | PoC 仅 UIKit，SwiftUI 不在范围 |
| NSLocalNetworkUsageDescription 弹窗打扰用户 | 🟢 | PoC 可接受，正式版考虑 Bonjour 发现 |
| Web 坐标转屏坐标（webScale / webViewOffset） | 🟢 | iOS 不需要坐标点击，问题自动消失 |

## 11. 下一步方向

### M1-M7 Agent Bridge 实施（约 6-10 天）

| 里程碑 | 工作量 | 验收 |
|---|---|---|
| M1 — HTTP server + ping | 1-2 天 | `curl http://<iPhone-IP>:9700/ping` 返回 version |
| M2 — Capture 走 HTTP | 半天 | `curl /capture` 返回完整 YAML |
| M3 — RefRegistry 基础设施 | 1-2 天 | `RefRegistry.shared.view(for: "n3")` 返回正确 UIView |
| M4 — Native ops（click/type/check/swipe） | 2-3 天 | AgentTestViewController 跑端到端 |
| M5 — Web ops（exec_js/click web ref/select_option） | 1-2 天 | Web 表单页提交、下拉、文本输入成功 |
| M6 — 剩余 ops + 收尾 | 1 天 | `scripts/test-web-ops.sh` 跑全操作 |
| M7 — Host 客户端打通 | 半天 | OpenCode 里说「点击 Web 表单页的提交按钮」→ 实际触发 |

### P0 Native Capture 质量提升

- `iOSRoleResolver` 加 UITableViewCell / UICollectionViewCell 父链判定
- 移植 generic 折叠算法（`NativeSemanticTreeBuilder.java` 有，iOS 没做）
- `[disabled]` 状态只对交互角色生效

### 跨端 Issue #001

修复 dom-serializer.js 不输出 ARIA props（href/placeholder），跨端一起改，约 0.5 天。详见 [`issues/001-web-dom-serializer-missing-props.md`](../issues/001-web-dom-serializer-missing-props.md)。

## 12. 不在 PoC 范围

- 跨 App 操作（iOS 沙箱禁止，需要越狱或 MDM 受管设备）
- XCTest UI 自动化路径（需要插桩 + 测试 host，不是 in-app）
- SwiftUI HostingView 内部点击
- Background mode / 后台保活
- App Store 合规（HTTP server / 私有 API 会被拒）
- 多设备管理 / 远程云手机

## 13. 关联

- 跨端对照：[`android-poc-summary.md`](android-poc-summary.md) / [`harmonyos-poc-summary.md`](harmonyos-poc-summary.md)
- 原始调研：[`ios-port-research.md`](ios-port-research.md)
- Agent Bridge 完整设计：[`ios-agent-bridge-plan.md`](ios-agent-bridge-plan.md)
- 阶段总结：[`ios-research-summary.md`](ios-research-summary.md)
- 踩坑清单（私有 memory）：`~/.claude/projects/.../memory/ios_poc_known_issues.md`
- iOS 源码：[`ios/UIPerception/`](../ios/UIPerception/)
