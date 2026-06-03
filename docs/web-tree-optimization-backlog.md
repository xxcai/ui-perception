# Web Tree 优化 Backlog

## 背景

Web 模式采用 Playwright 风格的 ref 分配（所有有 valid bounds 的节点都获得 ref），以支持后续 selector-based 操作。在真实 WeLink Web 页面上产生了 255 个 ref，其中 212 个是 generic 角色。虽然 agent 能自行筛选关键元素，但输出仍有优化空间。

**约束：不能折叠节点。** 后续 web 操作会使用 DOM 选择器（对齐 Playwright），折叠会丢失节点到 ref 的映射。

## 已解决

### ~~6. 特殊页面 DOM 序列化不完整~~ → 已修复 (commit e89efec)

**问题：** WeLink 客服聊天页面只有 6 个 ref，整棵 DOM 子树被误杀。

**根因：** `WebDomSerializer.isVisible()` 使用 `getBoundingClientRect()` 零尺寸作为可见性过滤条件。WeLink 页面的 `#root > SPAN`（`display:inline`）包裹了整个应用，其 bounding rect 为零尺寸，导致整棵子树被丢弃。

**修复：** 对齐 Playwright 的 `isElementHiddenForAria` 逻辑：
1. 去掉零尺寸 bounding rect 检查（Playwright 不检查）
2. 添加 `display:contents` 透明处理（递归检查子节点可见性）
3. 不可见父节点的可见子节点 reattach 到祖父容器（不丢弃子树）
4. 祖先链检查补充 `display:none`（之前只检查 `aria-hidden`）

**验证结果：** WeLink 聊天页从 6 个 ref 恢复到 172 个 ref，聊天内容、FAQ 列表、服务记录、底部输入栏全部正常输出。

## 待优化

### 1. Generic name 聚合过多子文本

**现状：** `generic "公司 协会 代表处 协会 研究所 协会 + 公司爱心协会 + 公司读书协会 ..."`
所有子节点的文本被拼接为父节点的 name，导致 name 过长且无语义区分度。修复 reattach 后更加明显——多层 generic 都聚合了子树全部文本。

**方案：** TreeNormalizer 中对 generic 节点的 name 生成策略优化——当 name 超过一定长度或包含过多子文本时，截断或只保留直接文本内容。

**优先级：** 高（reattach 修复后 name 聚合问题更严重）

### 2. 深层 DOM 嵌套冗余

**现状：** 一个协会卡片有 5 层 generic 嵌套（外层容器 → 内层容器 → 图标区 → 图标 → 文字），16 个协会产生 ~80 个 generic ref。

**方案：** 在不折叠节点的前提下，可以在渲染层（SnapshotRenderer）控制缩进层级，或在输出中标注哪些是"布局容器"vs"内容节点"。也可以通过标注 `layout` state 来帮助 agent 区分。

**优先级：** 中

### 3. 文本片段过度拆分

**现状：** "活动热量 0 /200 千卡" 被拆成 `generic "•"` + `generic "活动热量"` + `generic "0"` + `generic "/200 千卡"` 四个独立节点，每个都有 ref。

**方案：** TreeNormalizer 可合并连续的纯文本 generic 子节点为一个 text 节点（保留第一个子节点的 bounds）。注意：合并后的节点需要能反向映射到 DOM，以支持 selector 操作。

**优先级：** 高（对输出噪声影响最大）

### 4. Web accessibility tree 缺少语义 role

**现状：** 大量 HTML `<div>` 被映射为 `generic`，缺少语义信息（tab、navigation、article 等）。

**方案：** 增强 WebJsonParser 或 TreeNormalizer，利用 HTML 语义标签（`<nav>`、`<article>`、`<section>`、`<aside>`）或 ARIA role 生成更有意义的 SemanticRole。当前 web tree 已有 section/tab role，但覆盖不全。

**优先级：** 中

### 5. Web 点击操作对齐 Playwright selector

**现状：** 点击操作基于坐标（ref → bounds → adb tap）。

**方案：** 后续 web 操作改用 DOM selector（类似 Playwright 的 `aria-ref=eN` 选择器），需要：
- web tree 构建时记录 DOM 节点路径或唯一标识
- click endpoint 支持 selector-based 操作
- 可能需要在 web capture 时注入选择器逻辑

**优先级：** 高（架构决策点，影响 TreeNormalizer 设计）

### 7. Hidden tag 子节点通过 reattach 泄漏（新发现）

**现状：** reattach 修复后，SCRIPT 标签内的文本节点（如 `document.body.addEventListener('touchstart', function () { });`）被 reattach 到父容器，出现在 web tree 末尾。STYLE 标签内的 CSS 文本同理。

**根因：** `isHiddenForAria` 对 hidden tag（SCRIPT/STYLE 等）返回 true，但 `serialize` 仍然遍历其 childNodes 并将文本节点 reattach 到祖父容器。

**方案：** 在 `serialize` 中，对 hidden tag 的元素跳过子节点遍历（不做 reattach）。区分 "hidden tag"（直接跳过子树）和 "CSS 不可见"（reattach 子节点）两种情况。

**优先级：** 高（数据正确性问题）

### 8. Bounds 对零尺寸元素无效（新发现）

**现状：** reattach 后去掉了零尺寸 rect 过滤，导致大量 bounding rect 为 `[0,0,0,0]` 的元素进入输出。这些元素虽然 CSS 可见（如 `display:inline` 的 SPAN），但实际没有布局 box，bounds 信息无意义。

**方案：** bounds 为零时不输出 bounds（或在 RefAssigner 中不分配 ref），但元素本身仍保留在树中。与 Playwright 的 `computeBox` 行为一致——Playwright 也不给无 box 的元素分配 ref。

**优先级：** 中

## 关联文件

| 文件 | 职责 |
|------|------|
| `web-plugin/.../WebDomSerializer.java` | JS DOM 序列化脚本 |
| `web-plugin/.../WebJsonParser.java` | 解析 web accessibility JSON |
| `web-plugin/.../WebDebugScript.java` | 调试用 DOM 诊断脚本 |
| `perception-core/.../TreeNormalizer.java` | web tree 预处理 |
| `perception-core/.../RefAssigner.java` | ref 分配（web mode = Playwright 风格） |
| `perception-core/.../SnapshotRenderer.java` | tree → YAML 文本渲染 |
| `perception-sdk/.../PerceptionHttpServer.java` | HTTP Server（含 /debug-dom 调试端点） |
| `mobile-vision-pi/.pi/SYSTEM.md` | Pi-Agent 系统提示词 |
