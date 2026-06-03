# Web 页面视图识别

## 目标

为 ui-perception 项目添加 web 页面感知能力：JS 注入 DOM → ARIA 语义节点 JSON，transform 阶段统一输出 YAML。

## 关键决策

1. **PluginRegistry + canHandle()** — 插件自判断适用性，宿主零配置
2. **WebView 占屏 > 50%** — 遍历 View 树 + getGlobalVisibleRect
3. **JS 注入 ARIA tree** — Playwright 的 ariaSnapshot 模式，非原始 HTML
4. **格式统一** — 共享语义模型下沉 perception-core，native/web 复用同一 Renderer
5. **ref 前缀** — native 用 n1/n2，web 用 w1/w2
6. **States 格式** — 统一 key=value 格式，对齐 web（模型基于 web 训练更多）
7. **Composer 层融合** — native + web 扁平拼接，WebView 占位 + 索引标记

## 步骤

### Phase 1: Capture（已完成）

1. ✅ PerceptionPlugin 添加 canHandle() default 方法
2. ✅ 新建 PluginRegistry
3. ✅ 改造 CaptureHandler + CaptureFloatingButton 使用 Registry
4. ✅ PerceptionSdk.init() 注册 NativePerceptionPlugin
5. ✅ 改造 WebBaselinePlaceholderActivity 为全屏 WebView
6. ✅ 新建 WebViewFinder（占屏面积检测）
7. ✅ 真机验证基线页渲染 + WebView 检测
8. ✅ NativeSemanticRole 新增 LINK、HEADING、NAVIGATION
9. ✅ 新建 WebDomSerializer — ARIA tree JS 脚本
10. ✅ 真机测试 JS 脚本（通过 /capture-raw 端点验证）
11. ✅ 新建 WebPerceptionPlugin + SDK 注册
12. ✅ 真机验证（/capture-raw 返回 web JSON 正确）

### Phase 2: Transform + 格式统一（已完成）

详见 [20260603100000-phase2-transform.md](20260603100000-phase2-transform.md)

1. ✅ 共享类型下沉 perception-core + States 格式更新
2. ✅ Native input hint/value 改造
3. ✅ Web transform 实现（JSON → YAML）
4. ✅ 端到端验证

### Phase 3: Native + Web Fusion（进行中）

详见 [20260603100000-phase3-fusion.md](20260603100000-phase3-fusion.md)

1. PerceptionPlugin 添加 buildSemanticTree()
2. Native/Web 插件实现 buildSemanticTree()
3. 防止 WebView 节点折叠
4. 新建 SemanticFusion + WebFusionMode
5. 改造 CaptureHandler 融合流程
6. 端到端验证

## ARIA Role 映射表

| HTML tag / ARIA | JS 输出 role | SemanticRole |
|---|---|---|
| a[href] | link | LINK |
| button | button | BUTTON |
| input[type=text/email/…] | input | INPUT |
| input[type=checkbox] | checkbox | CHECKBOX |
| input[type=radio] | radio | RADIO |
| img | image | IMAGE |
| h1-h6 | heading | HEADING |
| ul/ol | list | LIST |
| li | listitem | LIST_ITEM |
| nav | navigation | NAVIGATION |
| section | section | SECTION |
| 其他可见元素 | generic | GENERIC |
| 纯文本节点 | text | TEXT |

## 非目标

- 不引入 AccessibilityService 或 CDP
- 不引入 Kotlin
