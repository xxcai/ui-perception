# iOS 端 UI Perception SDK 预研总结

## 背景

UI Perception 项目当前是 Android 原生实现，核心能力是把当前屏幕的 UI 树抓取为原始 XML，再转换为 LLM 友好的 YAML semantic snapshot。本项目不包含 Agent 决策，只做感知层数据准备。

为了评估该能力在 iOS / 鸿蒙上的可行性，先做 iOS 端的最小预研。预研的核心问题是：

> 在 iOS 上能否拿到等价于 Android `native_xml` 的 UI 结构数据，并产出同格式的 semantic snapshot？

本文是 PoC 跑通后的总结，覆盖：技术路线、跨端输出对比、关键坑、剩余工程项、跨平台架构建议。

## 预研决策

| 项 | 选择 | 理由 |
|---|---|---|
| 最低 iOS 版本 | iOS 14 | 兼容近 4 年设备，UISceneDelegate / Swift 6 都可用 |
| UI 框架 | UIKit | 与 Android View 系统最贴近，可同时验证 UIView 树遍历和 accessibility 路线 |
| 语言 | Swift 6 | 2026 年 Apple 平台默认选择，与现代 SDK 互操作最好 |
| 抓取路线 | UIView 树遍历 | 1:1 对应 Android `ViewHierarchyDumper` 路线 |
| 工程位置 | `ios/` 子目录 | 与 Android 工程零耦合，SPM/Xcode 互不影响 |
| 依赖管理 | XcodeGen + SPM | `project.yml` 声明式生成 xcodeproj，避免 pbxproj 冲突 |
| 预研范围 | 仅 native capture PoC | 不搬 composer / evaluation，只验证核心抓取链路 |

## 工程结构

```
ui-perception/
├── (Android 工程保持原样)
└── ios/
    ├── project.yml                              # XcodeGen 配置
    ├── UIPerception.xcodeproj/                  # 生成产物（gitignore）
    └── UIPerception/
        ├── App/                                 # app 入口
        │   ├── AppDelegate.swift
        │   ├── SceneDelegate.swift              # iOS 14+ UISceneSession
        │   ├── RootViewController.swift         # baseline 页面列表
        │   └── Info.plist
        ├── BaselinePages/
        │   └── MessageHomeViewController.swift  # 复刻 Android 消息首页
        └── Capture/                             # 算法层 + iOS 抓取层
            ├── SemanticRole.swift               # ← SemanticRole.java 逐行移植
            ├── Bounds.swift                     # ← Bounds.java
            ├── RoleDecision.swift               # ← RoleDecision.java
            ├── SemanticNode.swift               # ← SemanticNode.java
            ├── SnapshotRenderer.swift           # ← SnapshotRenderer.java
            ├── RefAssigner.swift                # ← RefAssigner.java
            ├── UIKitViewNode.swift              # iOS 抓取中间表示
            ├── UIKitHierarchyDumper.swift       # ← ViewHierarchyDumper.java
            ├── iOSRoleResolver.swift            # ← NativeRoleResolver.java (UIKit class 表)
            ├── SemanticTreeBuilder.swift        # ← NativeSemanticTreeBuilder.java
            ├── FloatingButton.swift             # 浮动感知按钮
            └── CaptureCoordinator.swift         # capture 入口
```

**代码量统计**：15 个 Swift 文件，~1100 LOC。其中 6 个文件是从 Java 逐行移植（算法层），4 个文件是 iOS 特有重写（抓取层 + UI）。

## 验证结果

### 跑通的端到端流程

1. `xcodebuild` 编译 `UIPerception.app`（iphonesimulator SDK）
2. `xcrun simctl install + launch` 启动到 iPhone 17 模拟器（iOS 26.3 runtime）
3. 用户点击浮动「感知」按钮 → 主线程同步触发 `UIKitHierarchyDumper.dump()`
4. 走 `SemanticTreeBuilder.build() → iOSRoleResolver → RefAssigner → SnapshotRenderer`
5. 写文件到 `<Documents>/captures/ios_message_home/runs/{ts}/native/transformed/native_semantic_snapshot_{ts}.yml`

### 跨端同页面输出对比

同一逻辑页面「消息首页」在 Android 和 iOS 上的 capture 结果：

| 维度 | Android 输出 | iOS PoC 输出 | 评价 |
|---|---|---|---|
| 节点总数 | ~135 行 YAML | 95 行 YAML | iOS 内部嵌套未折叠 |
| 列表容器 role | `scroll [scrollable]` | `list [scrollable]` | iOS 用 UITableView，Android 用 ScrollView 嵌 RecyclerView，差异在源 UI 不在抓取 |
| 列表项 role | `button`（推断可点） | `text` | iOS 应该是 `listitem`，role 表需补 UITableViewCell 父链判定 |
| 列表项 name | 各子 TextView 单独输出 | `accessibilityLabel` 合并为一句 | ✅ iOS 更聚合，更适合 LLM |
| 搜索框 role | `button`（被误判） | `input` | ✅ iOS 更准确 |
| 顶部按钮 | 3 个 button + 各自 image/text | 2 个 button + image/text | iOS baseline 页面简化，不是抓取问题 |
| Tab bar / chip 横滚 | 有 | 无 | iOS baseline 页面简化 |
| 输出格式 | YAML（自定义 serializer） | 同格式，同 serializer 算法 | ✅ 跨端完全等价 |

### iOS 端实际输出（节选）

```yaml
- screen:
  - generic:
    - screen:
      - button [ref=n1] [bounds=0,0,402,874]:        # root VC view
        - generic:
          - generic:
            - generic:
              - input "搜索" [ref=n2] [bounds=...]:   # UITextField ✓
              - button "新增" [ref=n5]:               # UIButton ✓
              - button "客服" [ref=n7]:               # UIButton ✓
              - list [scrollable] [ref=n9]:           # UITableView ✓
                - text "韩辰，在线":                   # UITableViewCell（accessibilityLabel）
                  - generic:
                    - generic [disabled]
                  - list [ref=n10]:                   # 内部 _UIListContentView（误判为 list）
                    - text "辰" [disabled]
                    - text "韩辰" [disabled]
                    - text "在线" [disabled]
                - text "李妍，请假":                   # 6 个 cell 都抓到了
                - text "孙瀚，外出":
                - text "周霖，在开会":
                - text "苏芸，刚到工位":
                - text "梁晓舟，明天上班":
        - toolbar: - text "消息首页"                   # UINavigationBar ✓
```

### 预研结论

**iOS 端 SDK 路线在技术上可行。** 三件核心事情都跑通：

1. **UIView 树遍历**：能稳定拿到完整 UI 结构
2. **Role 推导 + ref 分配**：UIKit class 表生效，可交互元素拿到 ref
3. **跨端算法等价性**：`SnapshotRenderer` 输出格式与 Android 完全一致，可直接共用下游 LLM 输入管线

剩下要补的都是工程问题，不是路线问题。

## 关键技术坑

PoC 过程中遇到的 4 个真实坑，对后续移植到鸿蒙也有参考价值。

### 1. 浮动按钮 window 抢占 keyWindow

`SceneDelegate` 里同时存在两个 window：app 主 window（`.normal` level）和浮动按钮 window（`.alert + 1` level）。最初 dumper 用 `scene.windows.first(where: { $0.isKeyWindow })` 找目标，结果抓到了浮动按钮的 window（只有 6 个节点）。

**修法**：dumper 显式过滤 `windowLevel == .normal`。

```swift
let appWindows = scene.windows.filter { $0.windowLevel == .normal }
let target = appWindows.first(where: { $0.isKeyWindow })
    ?? appWindows.first(where: { !$0.isHidden })
```

### 2. accessibilityTraits 跨层透传

UIButton 内部的 UILabel（titleLabel）默认会继承父控件的 `accessibilityTraits`。最初用 `view.accessibilityTraits.contains(.button)` 判定 clickable，结果 title label 也被识别为 button，导致 `button "感知"` 套 `button "感知"`。

**修法**：iOS 上 clickable 判定不靠 traits，只信任 `view is UIControl` 和 `view.gestureRecognizers`。

### 3. Swift Optional 被 `String(describing:)` 字符串化

```swift
var current: UIViewController? = viewController   // ← 重新包成 Optional
return String(describing: type(of: current))      // ← "Optional<UIViewController>"
```

baselineId 由此变成 `ios_optional<uiviewcontroller>`。这是 Swift 区别于 Java/Kotlin 的典型陷阱。

**修法**：解包后声明为非 Optional 类型。

### 4. loading 期间禁用按钮污染 capture 状态

浮动按钮在 loading 期间会设 `button.isUserInteractionEnabled = false` 防止重复点击。最初 capture 在 loading 中触发，导致 dump 出来的 snapshot 把按钮标成 `[disabled]`。

**修法**：capture 期间强制还原 `isUserInteractionEnabled = true`。

## 剩余工程项

按优先级：

### P0 — 让 iOS 输出和 Android 结构对齐

- **UITableViewCell / UICollectionViewCell 父链判定**：`iOSRoleResolver` 当前用类名子串匹配，匹配不到自定义子类（如 `MessageCell`）。需要追加 `view is UITableViewCell` 类型的运行时判定。
- **generic 折叠算法**：从 `NativeSemanticTreeBuilder.java` 移植。当前 iOS 输出 5-7 层嵌套，Android 折叠后 2-3 层。
- **`[disabled]` 限制到交互元素**：UILabel 默认 `isUserInteractionEnabled=false`，但不应该被标为 disabled。disabled 状态只对 button/input/switch/slider 等交互角色生效。

### P1 — 跨端一致性

- **补齐 baseline 页面**：Mail / Contacts / Business 3 个页面，验证 sectioned list、alphabetical index、card grid 等多种 UI 形态。
- **role 表覆盖更多 UIKit 控件**：当前表只覆盖 ~15 个常用类。补 `UIDatePicker`、`UIPageControl`、`UIRefreshControl`、`UIVisualEffectView`、`UIStackView` 嵌套容器等。
- **辅助验证路线**：补一条 `UIAccessibility` 树抓取（不是 UIView 树），对比两条路线在不同场景下的输出差异。这是 SwiftUI 时代的主要备选路线。

### P2 — 架构清理

- **拆 SPM 包**：当前所有代码塞在一个 app target。后续拆 `PerceptionCore`（跨平台算法）和 `NativePlugin`（iOS 抓取）两个 Swift package，复用模式与 Android `:perception-core` / `:native-plugin` 对齐。
- **复用 `PerceptionPlugin` 协议**：移植 Android 的多插件 plan / run / entry 结构，让 iOS 也能挂多个 capture 方向。
- **evaluation 模块**：PoC 阶段跳过，正式移植时把 `OnDeviceEvaluationRunner.java`（1155 LOC）拆解后移植。

## 跨平台架构建议

从这次 PoC 看出，UI Perception 的代码可以清晰分三层：

```text
┌─────────────────────────────────────────────────┐
│  L3: 平台抓取层（必须重写）                       │
│  - Android: ViewHierarchyDumper (反射 DecorView) │
│  - iOS:     UIKitHierarchyDumper (UIView 递归)   │
│  - 鸿蒙:    @ohos.accessibility 树 / uitest      │
└─────────────────────────────────────────────────┘
                    ↓ 平台无关中间表示
┌─────────────────────────────────────────────────┐
│  L2: Role 推导 + 树构造（半平台无关）             │
│  - RoleResolver: class-name 表 + 属性覆盖规则     │
│    （Android 与 iOS 表不同，算法相同）            │
│  - SemanticTreeBuilder / RefAssigner             │
│    （完全平台无关，可直接共享算法）                │
└─────────────────────────────────────────────────┘
                    ↓ SemanticNode 树
┌─────────────────────────────────────────────────┐
│  L1: 输出 + 评测（完全平台无关）                  │
│  - SnapshotRenderer (YAML 序列化)                │
│  - LLM Input Snapshot v1 标准格式                 │
│  - EvaluationRunner                              │
└─────────────────────────────────────────────────┘
```

**L1 + L2 算法层（约 750 LOC）可以三端共享**。只要每端实现一个 L3 抓取层 + 一份 class-name 到 role 的映射表，就能接入同一套感知管线。这次 iOS PoC 已经验证了 L1+L2 的跨端等价性。

## 对鸿蒙预研的启示

iOS 跑通后，鸿蒙预研的风险点收窄到 L3 抓取层一个问题上：

> **`@ohos.accessibility` 能否在 App 进程内被合法调用，并拿到第三方 App 的元素树？**

如果答案是肯定的，鸿蒙预研可以照搬 iOS 的 L2 算法和架构。
如果是否定的（例如只能感知自家 App，或只能在测试进程调用），鸿蒙方向需要降级方案：
- 降级 1：只能感知自家 App，做不了跨 App Agent
- 降级 2：必须依赖 uitest 进程，类似 iOS XCUITest 路线，App 内不能直接调用

建议鸿蒙预研第一步只验证这一个核心问题，不要先动 L1/L2 算法层。

## 环境要求备忘

后续在干净 macOS 上复现 iOS 预研环境的最小步骤：

```bash
# 1. 装 Xcode（App Store，约 10-15 GB）
# 2. 切换 xcode-select
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer

# 3. 下载 iOS Simulator runtime（约 8 GB）
xcodebuild -downloadPlatform iOS

# 4. 装 XcodeGen
brew install xcodegen

# 5. 生成工程 + 编译 + 启动
cd ios/
xcodegen generate
xcodebuild -project UIPerception.xcodeproj -scheme UIPerception \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build \
  CODE_SIGNING_ALLOWED=NO
xcrun simctl boot "iPhone 17"
xcrun simctl install booted build/Build/Products/Debug-iphonesimulator/UIPerception.app
xcrun simctl launch booted com.hh.uiperception.ios
```
