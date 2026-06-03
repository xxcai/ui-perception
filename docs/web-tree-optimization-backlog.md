# Web Tree 优化 Backlog

## 背景

Web 模式采用 Playwright 风格的 ref 分配（所有有 valid bounds 的节点都获得 ref），以支持后续 selector-based 操作。在真实 WeLink Web 页面上产生了 255 个 ref，其中 212 个是 generic 角色。虽然 agent 能自行筛选关键元素，但输出仍有优化空间。

**约束：不能折叠节点。** 后续 web 操作会使用 DOM 选择器（对齐 Playwright），折叠会丢失节点到 ref 的映射。

## 优化点

### 1. Generic name 聚合过多子文本

**现状：** `generic "公司 协会 代表处 协会 研究所 协会 + 公司爱心协会 + 公司读书协会 ..."`
所有子节点的文本被拼接为父节点的 name，导致 name 过长且无语义区分度。

**方案：** TreeNormalizer 中对 generic 节点的 name 生成策略优化——当 name 超过一定长度或包含过多子文本时，截断或只保留直接文本内容。

**优先级：** 中

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

### 6. 特殊页面 DOM 序列化不完整

**现状：** WeLink 客服聊天页面只有 6 个 ref（5 个聊天气泡只有用户 ID，无实际文本）。而 `screen` 的 name 包含了全部聊天内容。原因是当前 DOM 序列化脚本（`WebDomSerializer`）通过 `childNodes` 递归遍历 DOM，对以下场景失效：
- **虚拟滚动/懒加载**：不在视口的 DOM 节点可能被回收
- **Shadow DOM 动态渲染**：内容通过 Shadow DOM 或 JS 动态填充，DOM 结构只有容器无文本子节点
- **Canvas/自定义渲染**：不使用标准 DOM 节点展示文本

**方案：** 增强 `WebDomSerializer` 或提供备选采集方式：
- **方案 A**：JS 注入时增加 `innerText` 感知逻辑，对空子节点的容器尝试 `innerText` 提取
- **方案 B**：接入 Chrome DevTools Protocol（CDP）获取 WebView 的 accessibility tree，绕过 DOM 遍历限制
- **方案 C**：对特定框架（React/Vue 虚拟列表）做适配

**优先级：** 高（影响核心场景覆盖）

**验证页面：** WeLink 客服聊天页（`huawei.w3.h5.H5Activity`，内容为"12345客服"）

## 关联文件

| 文件 | 职责 |
|------|------|
| `web-plugin/.../WebJsonParser.java` | 解析 web accessibility JSON |
| `perception-core/.../TreeNormalizer.java` | web tree 预处理 |
| `perception-core/.../RefAssigner.java` | ref 分配（当前 web mode = Playwright 风格） |
| `perception-core/.../SnapshotRenderer.java` | tree → YAML 文本渲染 |
| `mobile-vision-pi/.pi/SYSTEM.md` | Pi-Agent 系统提示词 |
