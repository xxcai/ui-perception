# UI Perception 架构

## 模块结构

```
ui-perception/
├── perception-core/     # 核心类型与共享逻辑
├── native-plugin/       # Android Native 页面采集
├── web-plugin/          # WebView 页面采集
├── perception-sdk/      # SDK（HTTP Server + Fusion 调度）
├── app/                 # 测试 App（Baseline 页面）
├── baseline-pages/      # 基准测试页面
└── docs/                # 文档
```

## 数据流

```
App Activity
    │
    ├─ WindowManagerHelper.getFocusedWindowView()  ← 焦点窗口选择
    │   反射 WindowManagerGlobal.mViews
    │   找 hasWindowFocus()==true 的 View (Dialog/PopupWindow)
    │   回退到 activity.getDecorView()
    │
    ├─ Native View Tree XML ──→ NativePlugin
    │                              │
    │                              ├─ NativeViewXmlParser (XML → NativeViewNode)
    │                              ├─ NativeSemanticTreeBuilder (→ SemanticNode tree)
    │                              │   ├─ NativeRoleResolver (role 推导)
    │                              │   └─ states 填充
    │                              │
    │                              └─ buildSemanticTree() → SemanticNode
    │
    ├─ WebView Accessibility JSON ──→ WebPlugin
    │                                    │
    │                                    ├─ WebJsonParser (JSON → SemanticNode)
    │                                    ├─ TreeNormalizer (规范化)
    │                                    │
    │                                    └─ buildSemanticTree() → SemanticNode
    │
    └─ CaptureHandler (Fusion 调度)
         │
         ├─ 获取 native + web SemanticNode trees
         ├─ SemanticFusion.fuse(nativeTree, webTree)
         │   ├─ injectWebMarker (标记 webview [web])
         │   ├─ RefAssigner.assign(native, "n", webMode=false)  ← role 白名单
         │   ├─ RefAssigner.assign(web, "w", webMode=true)      ← Playwright 风格
         │   └─ SnapshotRenderer.render → YAML text
         │
         └─ HTTP Response: { activity, yaml }
```

## Fusion 输出格式

```
- screen:
  - generic "content":
    - webview [web] [ref=n1] [bounds=...]    ← Native 树，webview 占位
  - generic "navigationBarBackground"
  - generic "statusBarBackground"

--- Web ---                                  ← 分隔符

- screen "Page Title" [ref=w1] [bounds=...]:  ← Web 树
  - heading "Title" [ref=w2] [bounds=...]
  - link "a link" [clickable] [ref=w3] [bounds=...]
  - button "Submit" [clickable] [ref=w4] [bounds=...]
```

### Ref 前缀语义

| 前缀 | 来源 | 分配策略 |
|------|------|---------|
| `n` | Native | Role 白名单（BUTTON, INPUT, CHECKBOX, RADIO, SWITCH, SLIDER, PICKER, LIST, GRID, SCROLL, WEBVIEW） |
| `w` | Web | Playwright 风格：所有有 valid bounds 的元素 |

## 核心类

| 类 | 模块 | 职责 |
|----|------|------|
| `SemanticRole` | perception-core | Role 枚举（screen, toolbar, button, input, webview 等） |
| `SemanticNode` | perception-core | 不可变语义树节点 |
| `RefAssigner` | perception-core | ref 分配（支持 native/web 双模式） |
| `SemanticFusion` | perception-core | native + web 树融合 |
| `SnapshotRenderer` | perception-core | SemanticNode → YAML 文本 |
| `WindowManagerHelper` | native-plugin | 焦点窗口选择（反射 WindowManagerGlobal） |
| `NativeRoleResolver` | native-plugin | Android className → SemanticRole 推导 |
| `NativeSemanticTreeBuilder` | native-plugin | NativeViewNode → SemanticNode 转换 |
| `WebJsonParser` | web-plugin | Accessibility JSON → SemanticNode |
| `TreeNormalizer` | perception-core | Web tree 规范化 |
| `CaptureHandler` | perception-sdk | Fusion 调度入口 |
| `PerceptionHttpServer` | perception-sdk | HTTP Server（/capture, /click, /swipe） |

## Pi-Agent 集成

`mobile-vision-pi` 项目通过 Pi-Agent extension 连接：

- `.pi/SYSTEM.md` — Agent 系统提示词（包含 fusion 格式说明）
- `.pi/extensions/mobile-vision/` — capture/click/swipe 工具实现
- Agent 通过 `phone_capture_ui` 获取 snapshot，通过 `phone_click(ref)` 操作元素
