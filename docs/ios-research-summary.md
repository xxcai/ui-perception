# iOS 预研总结

> 范围：从 iOS 端 UI Perception SDK 可行性验证，到 Agent Bridge 方案设计，再到通信选型与 WDA 评估。
> 状态：PoC（Native + Web capture）已跑通；Agent Bridge 方案待实施。

## 1. 已完成事项

### 1.1 Native Capture PoC

| 项 | 结果 |
|---|---|
| iOS 最低版本 | iOS 14 |
| UI 框架 | UIKit |
| 语言 | Swift |
| 工程位置 | `ios/`（与 Android 完全隔离） |
| 抓取链路 | UIView 树 → UIKitHierarchyDumper → SemanticTreeBuilder → RefAssigner("n") → SnapshotRenderer |
| 输出 | 95 行 YAML，结构与 Android 对齐 |
| 验证 | iPhone 16 Pro Max 真机通过 |

### 1.2 Web Capture PoC

| 项 | 结果 |
|---|---|
| 抓取链路 | WKWebView → 注入 dom-serializer.js → JSON → WebJsonParser → TreeNormalizer → RefAssigner("w", webMode) → SnapshotRenderer |
| 输出 | 35 行 YAML，含 heading/textbox/combobox/button/list/listitem 全角色 |
| 中文文本 | ✅ UTF-8 完整保留 |
| 验证 | iPhone 16 Pro Max 真机通过 |

### 1.3 跨端一致性

iOS 输出与 Android 1:1 对齐（除 bounds 数值与 iOS 缺失 resource-id）。dom-serializer.js 跨端共享同一份文件，下游算法（RefAssigner / SnapshotRenderer / Bounds）逐行从 Java 移植到 Swift。

## 2. 关键技术决策

### 2.1 PoC 架构选择

| 选择项 | 决定 | 理由 |
|---|---|---|
| UI 框架 | UIKit（非 SwiftUI） | 与 Android View 1:1 对照；SwiftUI HostingView 拿不到内部 view |
| Capture 数据源 | UIView 树（非 Accessibility 树） | 与 Android View 树架构一致；AX 树被系统过滤丢失层级 |
| Ref 系统 | 与 Android 共享 `n1/n2/...`（native）、`w1/w2/...`（web） | 跨端评测脚本可复用 |
| 工程隔离 | `ios/` 子目录 + 独立 Xcode 工程 | Android Gradle 不扫 iOS；iOS Xcode 不扫 Android |

### 2.2 iOS 特有限制 → 解决方案

| 限制 | 影响 | 解决 |
|---|---|---|
| 触摸事件合成 | 不能像 Android 那样按坐标 dispatchTouchEvent | Agent 操作改用 ref → UIView → sendActions |
| App 沙箱 | 不能跨 App 操作 | PoC 仅驱动自身 App；跨 App 走 WDA 独立路径（见 §4） |
| `evaluateJavaScript` + 主线程 semaphore 死锁 | completion 永不回调 | RunLoop.current.run pump 替代 DispatchSemaphore.wait |
| WKWebView 加载本地 HTML 中文乱码 | `loadHTMLString` 内部按 ISO-8859-1 重编码 | 用 `loadFileURL(_:allowingReadAccessTo:)` |
| iOS 26.3 simulator 字体回归 | 中文字符渲染成 `?` | 真机验证（模拟器仅做功能联调） |
| Floating button window 抢 key window | `currentViewController()` 返回 nil | normal window fallback；filter `windowLevel == .normal` |

完整踩坑清单见 memory：`ios_poc_known_issues.md`。

## 3. Agent Bridge 方案（设计完成，未实施）

### 3.1 方案选型

**选定**：in-app HTTP server via `Network.framework`，端口 9700（与 Android 对齐，host 客户端零改动）。

**否决项**：
- WebDriverAgent（见 §4）
- 第三方 HTTP 库（GCDWebServer / SwiftNIO）—— PoC 不引入依赖

### 3.2 核心架构差异（vs Android）

| 维度 | Android | iOS |
|---|---|---|
| Click 模型 | 坐标驱动（ref→bounds→dispatchTouchEvent） | 对象驱动（ref→UIView→sendActions） |
| Ref 缓存 | `RefBoundsCache` 存 `[ref: bounds]` | `RefRegistry` 存 `[ref: weak UIView]` |
| Auto-init | ContentProvider trick | `AppDelegate.didFinishLaunching` 直接调 |
| IPC 通道 | adb forward + HTTP + ContentProvider | HTTP only |

### 3.3 操作清单（15 个，跨端对齐）

`ping` / `capture` / `capture-raw` / `debug-dom` / `click` / `long_press` / `swipe` / `type_text` / `check` / `uncheck` / `select_option` / `press_key` / `exec_js` / `open_uri`

iOS 实现难度评估、7 个里程碑（M1-M7，约 6-10 天）、风险矩阵见 [docs/ios-agent-bridge-plan.md](./ios-agent-bridge-plan.md)。

## 4. WebDriverAgent 评估（结论：不采用）

| 维度 | WDA 方案 | 我们 in-app 方案 |
|---|---|---|
| 跨 App 操作 | ✅ 任意 App | ❌ 仅自身 |
| Capture 数据源 | ⚠️ Accessibility 树（被过滤） | ✅ UIView 树（原始层级） |
| ref 一致性 | ❌ 与现有 `n/w` ref 系统冲突 | ✅ 复用 |
| iOS 升级稳定性 | 🔴 经常断 | 🟢 App 内代码不受影响 |
| 部署复杂度 | 🔴 装 Appium + build WDA + 签名 | 🟢 App 启动即可 |
| 速度 | 🟡 IPC ~100-500ms | 🟢 <10ms |

**致命冲突**：WDA 只看 Accessibility 树，与我们 UIView 树 capture 不兼容。混合方案也不可行（两边 ref 语义不同）。WDA 适合「LLM agent 操作任意 App」的独立项目，不适合作为 SDK 内 capture+ops 闭环的组件。

## 5. Mac ↔ iOS 通信方案

iOS 无 `adb forward` 等价物（沙箱禁止 App 把端口暴露给 usbmuxd）。

| 方案 | 推荐度 | 场景 |
|---|---|---|
| **WiFi/LAN**（同子网） | 🟢 PoC 首选 | Mac 与 iPhone 同 Wi-Fi，直接访问 LAN IP |
| **USB 个人热点** | 🟢 备选 | Wi-Fi client isolation 场景；走 USB 物理层 |
| pymobiledevice3 tunnel | ❌ | 只能到 Apple 私有 CoreDevice 服务，到不了 App 端口；私有 entitlement 会被 App Store 拒审 |

PoC 选方案 1，Info.plist 加 `NSLocalNetworkUsageDescription` + ATS 例外。

## 6. 已知遗留问题

### Issue #001 — dom-serializer.js 不输出 ARIA props

跨端（Android + iOS 共享同一份 JS）web capture 输出**比 Playwright 少了关键导航/输入线索**：

- `link` 节点看不到 href（无法判断页内 vs 跳外）
- `textbox` 节点看不到 placeholder（空输入框丢失提示）

根因：dom-serializer.js 只在 `getName()` fallback 链里用这些属性，没复刻 Playwright ariaSnapshot.ts L185-193 的 props 输出逻辑。

修复方案（跨端一起改，5 步）见 [issues/001-web-dom-serializer-missing-props.md](../issues/001-web-dom-serializer-missing-props.md)。

### P0 工程项（iOS 端 Native Capture 质量提升）

- `iOSRoleResolver` 加 UITableViewCell / UICollectionViewCell 父链判定
- 移植 generic 折叠算法（`NativeSemanticTreeBuilder.java` 有，iOS 没做）
- `[disabled]` 状态只对交互角色生效

### P1 跨端一致性

- 补 Mail / Contacts / Business baseline 页
- iOS role 表覆盖更多 UIKit 控件
- accessibility 树抓取作为备选路线（SwiftUI 时代）

## 7. 文档索引

| 文档 | 内容 |
|---|---|
| [docs/ios-port-research.md](./ios-port-research.md) | PoC 研究报告（Native Capture 部分） |
| [docs/ios-agent-bridge-plan.md](./ios-agent-bridge-plan.md) | Agent Bridge 完整设计 + 7 个里程碑 |
| [issues/001-web-dom-serializer-missing-props.md](../issues/001-web-dom-serializer-missing-props.md) | href/placeholder 跨端缺失问题 |
| `ios/UIPerception/` | iOS 工程源码 |
| `~/.claude/projects/.../memory/ios_poc_known_issues.md` | 踩坑清单（私有 memory） |

## 8. 下一步建议（按优先级）

1. **决策**：是否启动 Agent Bridge 实施（M1-M7，约 6-10 天）。方案已就绪。
2. **修复 Issue #001**（跨端一起改，约 0.5 天）—— 让 web capture 输出达到 Playwright 信息密度
3. **Native Capture P0 工程项**（约 2-3 天）—— role 表 + generic 折叠 + disabled 状态
4. **启动鸿蒙 PoC**（iOS PoC 已验证跨端可行性，鸿蒙可参考架构）

iOS PoC 阶段产出：能跑、跨端一致、踩坑清单完整，可以转入实施期。
