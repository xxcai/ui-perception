# Android UI Perception PoC 总结

> 范围：作为跨端 PoC 的**基准实现**，描述当前 Android 端 native + web capture、agent bridge、touch dispatch 的完整形态，供 iOS / 鸿蒙两端对照
> 状态：✅ Native + Web 抓取链路、Agent Bridge、Touch Dispatch 全部生产可用
> 基线时间：2026-06（持续演进）

## 1. 角色定位

| 维度 | 说明 |
|---|---|
| 跨端角色 | 基准实现（reference impl），iOS / 鸿蒙所有移植都以本端为对照 |
| 项目角色 | 主平台，所有架构演进先在 Android 落地，再跨端移植 |
| 算法层共享 | `perception-core` 的 `Bounds` / `SemanticRole` / `SemanticNode` / `RefAssigner` / `SnapshotRenderer` / `WebJsonParser` 是三端共享源（iOS / 鸿蒙 1:1 移植） |
| 资产共享 | `dom-serializer.js`（718 行 IIFE）三端 byte-level 一致，MD5 = `76793967ba7c96ff4aae193ae344d677` |

## 2. 两条抓取路径

### 2.1 Native（Android View XML）

**数据源**：`WindowManagerHelper.getFocusedWindowView(activity)` → 反射 `WindowManagerGlobal.mViews`，挑选 `hasWindowFocus()==true` 的 View（Dialog/PopupWindow 优先），回退 `activity.getDecorView()`

**转换链路**：

```
focusWindow → DecorView
  ↓
ViewHierarchyDumper.dump() → raw XML  (~22KB / 消息首页)
  ↓
NativeViewXmlParser → NativeViewNode 树（保留 class/text/content-desc/resource-id/bounds/clickable/long-clickable/focusable/enabled/checked/selected/focused/scrollable/password）
  ↓
NativeSemanticTreeBuilder
  ├─ NativeRoleResolver   (className → SemanticRole)
  ├─ states 填充
  └─ generic wrapper 折叠
  ↓
RefAssigner (native mode, prefix="n") → role 白名单 + valid bounds
  ↓
SnapshotRenderer → YAML  (~4.5KB / 消息首页，相比 raw 压缩 ~82%)
```

**ref 白名单**（只给可操作 role 分配 ref）：`button / input / checkbox / radio / switch / slider / picker / list / grid / scroll / listitem / webview`

**集合控件特殊处理**：`RecyclerView.addOnItemTouchListener` / `ListView.setOnItemClickListener` 让 item 可点但 raw XML 不可见。三态：
- `clickable`：item 自身 `clickable=true`
- `clickable-inferred`：容器信号推断（`has-item-click-listener=true` / `has-item-touch-listener=true`）
- `clickable-guessed`：内部中间态，渲染时统一映射为 `clickable-inferred`

**clickable 推断增强**：反射 `onTouchEvent` override 判定（commit `7ca18f5`）—— 子类化 View 重写 onTouchEvent 即视为可交互

### 2.2 Web（WebView DOM）

**数据源**：`WebViewFinder.findWebView(decorView)` → 遍历找 `android.webkit.WebView` 实例 → 注入 `dom-serializer.js`（IIFE，同步返回 JSON）

**关键资产**：`web-plugin/src/main/assets/dom-serializer.js`（718 行，三端共享）

**Playwright 兼容性**：
- ref 分配策略与 Playwright ariaSnapshot 对齐（所有 valid bounds 元素都拿 ref）
- DOM 可见性判断复刻 Playwright 的 `isElementHiddenForAria`：`display:contents` 透传、不可见父节点的可见子节点 reattach 到祖父容器
- 输出 aria tree 风格 YAML，与 Playwright `ariaSnapshot()` 字段对齐

**TreeNormalizer 后处理**：web tree 进入融合前做规范化（generic name 聚合控制、文本片段合并、ARIA role 推断）

### 2.3 Fusion（native + web 拼接）

```
CaptureHandler.fuse(nativeTree, webTree)
  ├─ injectWebMarker (标记 webview [web])
  ├─ RefAssigner.assign(native, "n", webMode=false)  ← role 白名单
  ├─ RefAssigner.assign(web, "w", webMode=true)      ← Playwright 风格
  └─ SnapshotRenderer.render → 单一 YAML，分两段（native 在前，--- Web --- 分隔）
```

ref 前缀语义：`n*` = native role 白名单节点；`w*` = web 所有 valid bounds 节点

## 3. Agent Bridge

### 3.1 传输架构

```
┌──────────────┐  HTTP/JSON   ┌────────────────────────────────┐
│ Host Agent   │ ───────────► │ Android App                    │
│ (Mac/Cloud)  │              │                                │
│ OpenCode TS  │ ◄─────────── │  ┌──────────────────────────┐  │
│ curl/Python  │              │  │ PerceptionHttpServer     │  │
└──────────────┘              │  │ (port 9700, ServerSocket)│  │
       ↑                      │  └────────┬─────────────────┘  │
       │ adb forward tcp:9700 │           │                     │
       │ tcp:9700             │  ┌────────▼─────────────────┐  │
       │                      │  │ OperationRouter          │  │
       │                      │  │ ├─ CaptureHandler        │  │
       │                      │  │ ├─ RawCaptureHandler     │  │
       │                      │  │ ├─ TouchHandler          │  │
       │                      │  │ ├─ WebViewExecutor       │  │
       │                      │  │ ├─ DebugDomHandler       │  │
       │                      │  │ ├─ DebugExecJsHandler    │  │
       │                      │  │ └─ OpenUriHandler        │  │
       │                      │  └────────┬─────────────────┘  │
       │                      │  ┌────────▼─────────────────┐  │
       │                      │  │ RefBoundsCache           │  │
       │                      │  │ [ref → bounds]            │  │
       │                      │  │ + webElementIdx           │  │
       │                      │  └──────────────────────────┘  │
       │                      │                                │
       │ adb shell content    │  ┌──────────────────────────┐  │
       │ call ... (副通道)    │  │ ContentProvider           │  │
       └─────────────────────►│  │ {appId}.perception-sdk.init│ │
                              │  │ auto-init + 副通道指令    │  │
                              │  └──────────────────────────┘  │
                              └────────────────────────────────┘
```

### 3.2 双通道设计

| 通道 | 用途 | 备注 |
|---|---|---|
| **HTTP server**（port 9700） | host agent 主路径，所有 ops | host 通过 `adb forward tcp:9700 tcp:9700` 映射到本机 |
| **ContentProvider** | (1) auto-init（App 启动自动拉起 SDK）；(2) `adb shell content call` 直发指令，绕过 HTTP 链路 | host 在 server 未起时可降级用 |

### 3.3 Auto-init 路径

`PerceptionSdkInitProvider`（继承 `ContentProvider`）在 `onCreate()` 调 `PerceptionSdk.init(context)`，无需 Application 子类化或 manifest 改动。ContentProvider 在 App 启动时早于 Activity 被系统创建。

### 3.4 完整操作清单（15 个）

| 操作 | 实现要点 | 备注 |
|---|---|---|
| `ping` | 版本/连通性 | - |
| `capture` | 走 CaptureCoordinator，返回 YAML + 缓存 ref→bounds | 必须先于其他 ops |
| `capture-raw` | 各插件原始数据（native XML / web JSON） | - |
| `debug-dom` | web tree 原始 JSON | 调试用 |
| `click` | `TouchHandler.click` → DOWN/UP MotionEvent，50ms 间隔 | 坐标驱动 |
| `long_press` | 同上，可配 duration（默认 500ms） | - |
| `swipe` | 10 步插值 MOVE event，300ms 总时长 | - |
| `type_text` | `EditText.setText()`（不是 `dispatchKeyEvent`） | commit `6f9f991` 踩坑修复 |
| `check` / `uncheck` | Switch/Checkbox 反射调 setOn + send click | - |
| `select_option` | 仅 web：JS dispatch change | - |
| `press_key` | `back` 可靠；`home`/`menu` 受系统限制 | 默认 back only |
| `exec_js` | WebView.evaluateJavascript + CountDownLatch 5s | - |
| `open_uri` | host app routing | commit `5c95242` |

### 3.5 RefBoundsCache

`capture` 完成后写入 `[ref → [l,t,r,b]]`，所有 ops 通过 ref 查 bounds → 计算中心点 → 注入事件。`webElementIdx` 用于 web 操作时定位 DOM 节点。

## 4. 工程结构

```
ui-perception/
├── perception-core/                  # 跨端算法层（三端共享源）
│   └── com.hh.uiperception.core/
│       ├── SemanticRole.java         # 50 个 role 枚举
│       ├── Bounds.java               # int rect
│       ├── RoleDecision.java         # role + source + confidence
│       ├── SemanticNode.java         # 不可变语义树节点
│       ├── RefAssigner.java          # native + web 双模式
│       ├── SnapshotRenderer.java     # SemanticNode → YAML
│       ├── WebJsonParser.java        # web accessibility JSON → SemanticNode
│       └── TreeNormalizer.java       # web tree 规范化
│
├── native-plugin/                    # Android Native 抓取层
│   └── com.hh.uiperception.nativeplugin/
│       ├── NativePerceptionPlugin.java
│       ├── ViewHierarchyDumper.java       # 反射 WindowManagerGlobal
│       ├── WindowManagerHelper.java       # 焦点窗口选择
│       ├── NativeViewXmlParser.java       # XML → NativeViewNode
│       ├── NativeRoleResolver.java        # className → SemanticRole
│       └── NativeSemanticTreeBuilder.java # generic 折叠 + listitem 推断
│
├── web-plugin/                       # WebView 抓取层
│   └── com.hh.uiperception.webplugin/
│       ├── WebPerceptionPlugin.java
│       ├── WebViewFinder.java
│       ├── WebDomSerializer.java          # 注入 + 接收 dom-serializer.js JSON
│       ├── WebJsonParser.java
│       ├── TreeNormalizer.java
│       └── src/main/assets/
│           └── dom-serializer.js          # ⭐ 三端共享，718 行 IIFE
│
├── perception-sdk/                   # Agent Bridge
│   └── com.hh.uiperception.sdk/
│       ├── PerceptionSdk.java             # SDK 入口
│       ├── PerceptionSdkInitProvider.java # auto-init via ContentProvider
│       └── internal/
│           ├── PerceptionHttpServer.java  # port 9700 ServerSocket
│           ├── CaptureHandler.java
│           ├── RawCaptureHandler.java
│           ├── OperationHandler.java
│           ├── TouchHandler.java          # MotionEvent dispatch
│           ├── WebViewExecutor.java       # evaluateJavascript 包装
│           ├── WebActionScript.java       # web click/change JS 脚本
│           ├── DebugDomHandler.java
│           ├── DebugExecJsHandler.java
│           ├── OpenUriHandler.java
│           ├── ForegroundActivityTracker.java
│           ├── RefBoundsCache.java        # ref → bounds
│           ├── CaptureResponse.java
│           └── OperationResponse.java
│
├── app/                              # 测试 App（baseline 页面入口）
├── baseline-pages/                   # 4 个 native baseline + WebView baseline
├── evaluation/                       # 端上评测
├── samples/agent-bridge/             # JS Bridge + adb reverse 早期样本
└── docs/                             # 文档
```

## 5. 验证结果

### Native Transform 数据量

四个 native 首页真机抓取（[`android-native-xml-transform-route.md`](android-native-xml-transform-route.md) §数据量对比）：

| 页面 | Raw XML | Snapshot | 压缩比 |
|---|---:|---:|---:|
| 消息 | 28,538 B | 4,784 B | 16.8% |
| 邮件 | 21,594 B | 4,225 B | 19.6% |
| 通讯录 | 22,162 B | 4,000 B | 18.0% |
| 业务 | 17,835 B | 3,315 B | 18.6% |
| **合计** | **90,129 B** | **16,324 B** | **18.1%** |

平均压缩 ~82%，保留核心文本 / role / 状态 / ref / bounds。

### Native 输出样例

```yaml
- screen:
  - generic "content":
    - button "搜索" [ref=n4] [bounds=42,312,870,407]
    - button [ref=n5] [bounds=870,305,1038,415]:
      - image
      - text "客服"
    - scroll [scrollable] [ref=n14] [bounds=0,636,1080,2190]:
      - generic:
        - button [ref=n16] [bounds=0,825,1080,1014]:
          - text "辉"
          - generic:
            - text "梁晓舟"
            - text "明天上班"
```

### Web 输出样例

```yaml
- screen "Page Title" [ref=w1] [bounds=...]:
  - heading "Title" [ref=w2]
  - link "a link" [clickable] [ref=w3]
  - button "Submit" [clickable] [ref=w4]
```

## 6. 跨端架构对齐

```
                ┌─────────────────────────────────────┐
                │  Shared algorithm layer (1:1 port)  │
                │                                     │
                │  Bounds / SemanticRole / Node       │
                │  RefAssigner / SnapshotRenderer     │
                │  WebJsonParser / TreeNormalizer     │
                │  dom-serializer.js (718 行 IIFE)    │
                └────────┬───────────────┬────────────┘
                         │               │
            ┌────────────▼───┐   ┌───────▼────────┐
            │ iOS side       │   │ Harmony side    │
            │ UIKitHierarchy │   │ HarmonyInspector│
            │ Dumper         │   │ Parser          │
            │ iOSRoleResolver│   │ HarmonyRole     │
            │                │   │ Resolver        │
            │ WKWebView +    │   │ ArkUI Web +     │
            │ evaluateJS     │   │ javaScriptProxy │
            └────────────────┘   └─────────────────┘
                         ↑
                         │ Android 是源
                         │
                ┌────────┴────────────┐
                │ Android side        │
                │ ViewHierarchyDumper │
                │ NativeRoleResolver  │
                │ WebView + evalJS    │
                └─────────────────────┘
```

**对齐情况**：
- ✅ Bounds：三端 `[l, t, r, b]` int 格式一致
- ✅ RefAssigner：native prefix="n" + web prefix="w" 三端一致
- ✅ SnapshotRenderer：YAML 缩进 / 状态标记 / ref 语法一致
- ✅ dom-serializer.js：MD5 完全一致（`76793967ba7c96ff4aae193ae344d677`）
- ⚠️ Role 映射表：三端来源不同（Android View class / iOS UIKit class / ArkUI `$type`）

## 7. 踩坑记录

| 坑 | 表现 | 根因 | 修复 |
|---|---|---|---|
| Dialog-themed activity 点击 Y 坐标偏移 | 点击位置不对 | `activity.dispatchTouchEvent` 路径不转坐标系，仅在 decorView 在 (0,0) 时正确 | 统一走 `rootView.dispatchTouchEvent` + `getLocationOnScreen` 偏移转换（commit `8c69af0`） |
| 焦点窗口捕获遗漏 | Dialog/PopupWindow 内容抓不到 | 直接用 `activity.getDecorView` 漏掉浮层 | 反射 `WindowManagerGlobal.mViews` 找 `hasWindowFocus()==true` 的 View（commit `e4d1ad7` / `bf7779e`） |
| `dispatchKeyEvent` 输入文本不稳定 | 中文 / 复杂文本丢失 | IME 路径不可靠 | 改用 `EditText.setText()`（commit `6f9f991`） |
| 集合控件 item clickable=false 但实际可点 | listitem ref 丢失 | `RecyclerView.addOnItemTouchListener` 不写 clickable 字段 | 三态推断 + 父子双 ref（`clickable-inferred`） |
| dom-serializer inline generic 优化丢文本 | web 文本节点被误删 | 折叠算法激进 | commit `3772ffa` |
| WeLink 聊天页 DOM 整树被丢 | 只有 6 个 ref | `getBoundingClientRect` 零尺寸过滤误杀 `display:inline` SPAN | 对齐 Playwright `isElementHiddenForAria`：去掉零尺寸检查 + `display:contents` 透传 + reattach（commit `e89efec`） |
| ContentProvider 调用 handler 反射失败 | 副通道路径找不到方法 | DebugExecJs/OpenUri handler 默认包私有 | 改 public（commit `fdd2cc5`） |

## 8. 工程限制与决策

| 限制 | 当前应对 |
|---|---|
| `dispatchTouchEvent` 需要在主线程 | `runOnUiThread` + `CountDownLatch` 同步等待 |
| WebView `evaluateJavascript` 异步 | `CountDownLatch` 5s 超时同步 |
| App 后台时 HTTP server 失效 | 前台服务保活（业务方集成时声明） |
| 跨 App 操作无系统限制 | Android 本就支持，无需特殊处理 |
| ContentProvider 副通道需要 host 主动 adb shell | 文档化，作为 fallback |
| IME 输入法路径不可靠 | `EditText.setText()` 直填，绕过 IME |

## 9. 已知遗留问题

### Web Tree 优化（[`web-tree-optimization-backlog.md`](web-tree-optimization-backlog.md)）

| 优先级 | 问题 |
|---|---|
| 高 | Generic name 聚合过多子文本（reattach 后加剧） |
| 高 | 文本片段过度拆分（"活动热量 0/200 千卡" 被拆 4 个节点） |
| 高 | Hidden tag 子节点通过 reattach 泄漏（SCRIPT/STYLE 内容流出） |
| 高 | Web 点击对齐 Playwright selector（架构决策点） |
| 中 | 深层 DOM 嵌套冗余（5 层 generic × 16 卡片 = ~80 ref） |
| 中 | Bounds 对零尺寸元素无效 |
| 中 | Web accessibility tree 缺少语义 role（大量 div→generic） |

### Issue #001 — dom-serializer.js 不输出 ARIA props

跨端（Android + iOS + 鸿蒙共享同一份 JS）web capture 输出比 Playwright 少了关键导航/输入线索：
- `link` 节点看不到 href（无法判断页内 vs 跳外）
- `textbox` 节点看不到 placeholder（空输入框丢失提示）

详见 [`issues/001-web-dom-serializer-missing-props.md`](../issues/001-web-dom-serializer-missing-props.md)。

## 10. 关键样本

- `captures/native_home_mail/runs/{runId}/native/raw/native_xml_{ts}.xml`
- `captures/native_home_mail/runs/{runId}/native/transformed/native_semantic_snapshot_{ts}.yml`
- `captures/native_home_mail/runs/{runId}/evaluation/evaluation-result.json`

`baseline-pages/` 含 4 个 native baseline（消息 / 邮件 / 通讯录 / 业务）+ WebView baseline。

## 11. 关键提交记录

| Commit | 主题 |
|---|---|
| `3772ffa` | fix: inline generic optimization drops text content in dom-serializer |
| `5c95242` | feat: add /open_uri HTTP endpoint for host app routing |
| `c461f56` | feat: add ContentProvider transport via call() for adb shell access |
| `6f9f991` | fix: rewrite native text input via EditText.setText() instead of dispatchKeyEvent |
| `7ca18f5` | feat: detect onTouchEvent override for clickability inference |
| `e4d1ad7` | feat: capture focused window to support Dialog/PopupWindow scenarios |
| `bf7779e` | fix: dispatch touch events to focused window for Dialog/PopupWindow |
| `8c69af0` | fix: correct click coordinates when window is offset from screen origin |
| `e9fc109` | test: add NonFullscreenTestActivity for window-offset click verification |
| `aab878c` | feat: add agent bridge sample with adb reverse communication |

## 12. 关联

- 跨端对照：[`harmonyos-poc-summary.md`](harmonyos-poc-summary.md) / [`ios-poc-summary.md`](ios-poc-summary.md)
- 架构总览：[`architecture.md`](architecture.md)
- Native 转换细节：[`android-native-xml-transform-route.md`](android-native-xml-transform-route.md)
- ADB 调试：[`adb-http-debugging.md`](adb-http-debugging.md)
- Web 优化：[`web-tree-optimization-backlog.md`](web-tree-optimization-backlog.md)
- Host 客户端：`.opencode/plugins/phone-auto-test.ts` / `scripts/test-web-ops.sh`
- 早期 JS Bridge 样本：[`samples/agent-bridge/`](../samples/agent-bridge/)
