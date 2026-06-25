# Issue #001: dom-serializer.js 不输出 ARIA props（href/placeholder）

## 状态

Open

## 影响

跨端（Android + iOS）web 抓取输出**比 Playwright 少了关键导航/输入线索**。对依赖 ariaSnapshot 的 LLM agent 来说：

- **link** 节点看不到 URL → 无法判断「页内跳转」vs「跳外站」，点击前不知道去哪
- **textbox** 节点看不到 placeholder → 空输入框时丢失「该框要填什么」的唯一提示

## 复现

iOS 真机抓取 `web_form.html`（Android 端用同一份 dom-serializer.js，输出一致）。

### HTML 源（关键片段）

```html
<input id="email" type="email" placeholder="example@domain.com">
<a href="https://example.com/about" target="_blank">关于本页面</a>
```

### 当前 YAML 输出（iOS / Android）

```yaml
- textbox "邮箱" [clickable] [ref=w13] [bounds=32,340,408,381]
- link "关于本页面" [clickable] [ref=w34] [bounds=175,1084,254,1104]
```

placeholder 和 href 完全丢失。

### Playwright 实际输出（应该是这样）

```yaml
- textbox "邮箱":
  - /placeholder: example@domain.com
- link "关于本页面":
  - /url: https://example.com/about
```

## 根因

dom-serializer.js 文件头写着 "Design reference: Playwright injected ARIA snapshot pipeline"，但**没复刻 Playwright 的 props 输出逻辑**。

### Playwright 源码证据

`/Users/caixiao/Workspace/projects/playwright/packages/injected/src/ariaSnapshot.ts`：

```typescript
// 第 185-188 行：link + href → 输出 url prop
if (ariaNode.role === 'link' && element.hasAttribute('href')) {
  const href = element.getAttribute('href')!;
  ariaNode.props['url'] = href;
}

// 第 190-193 行：textbox + placeholder（且 placeholder != name）→ 输出 placeholder prop
if (ariaNode.role === 'textbox'
    && element.hasAttribute('placeholder')
    && element.getAttribute('placeholder') !== ariaNode.name) {
  const placeholder = element.getAttribute('placeholder')!;
  ariaNode.props['placeholder'] = placeholder;
}
```

渲染为 YAML 时（第 678-679 行）：

```typescript
for (const [name, value] of Object.entries(ariaNode.props))
  lines.push(indent(depth + 1) + '- /' + name + ': ' + yamlEscapeValueIfNeeded(value));
```

格式是 `- /propName: value`，作为父节点的子行。

### 我们的 dom-serializer.js 怎么处理这些属性

`/Users/caixiao/Workspace/projects/ui-perception/web-plugin/src/main/assets/dom-serializer.js`（Android + iOS 共用同一份）：

- 第 393-396 行：placeholder 只在 `getName()` 里作为 accessible name 的 fallback。如果 input 已有 `<label>` 关联（ARIA 命名优先级更高），placeholder 永远不会被输出。
- href：完全没单独抓，只在第 135 行用于判定 `<a>` 是不是 link（有 href 才算 link）。
- 完全没有 props 字段的概念，输出 JSON 只有 `{role, name, states, bounds, __pr_idx, children}`。

### 测试样本验证

Playwright 测试用例（`tests/page/page-aria-snapshot.spec.ts`）里有大量 `- /url: about:blank` 和 `- /placeholder: ...` 的预期输出，证明这是稳定 API 而非偶然行为。

## 修复方案

最小改动，1:1 对齐 Playwright：

### 1. dom-serializer.js（共享文件，一次改，两端都生效）

在 `serialize()` 函数构造节点时，加一段 props 提取逻辑，参考 Playwright ariaSnapshot.ts 第 185-193 行：

```javascript
// 在节点构造完成后、push 到 parent.children 之前
var props = {};
if (role === 'link' && el.hasAttribute('href')) {
  props.url = el.getAttribute('href');
}
if (role === 'textbox'
    && el.hasAttribute('placeholder')
    && el.getAttribute('placeholder') !== name) {
  props.placeholder = el.getAttribute('placeholder');
}
// props 字段加入返回的 node 对象
node.props = props;
```

### 2. SemanticNode（perception-core，Java + Swift 都改）

加一个 `props: Map<String, String>` 字段（或 `JSONObject` / `[String: String]`）。

### 3. WebJsonParser（Java + Swift 都改）

解析 JSON 时把 `props` 字段读进 SemanticNode.props。

### 4. TreeNormalizer（Java + Swift 都改）

如果 generic 折叠逻辑会复制节点，记得把 props 也带过去（PoC 阶段可暂不处理）。

### 5. SnapshotRenderer（Java + Swift 都改）

输出 YAML 时，节点如果有 props，按 Playwright 格式输出子行：

```yaml
- link "关于本页面" [clickable] [ref=w34] [bounds=...]:
  - /url: https://example.com/about
```

注意：props 应该在 `[ref=]` `[bounds=]` 同一行之后加 `:`，然后子行用 `- /key: value`。

## 验收

跑同一份 `web_form.html`，期望输出：

```yaml
- link "关于本页面" [clickable] [ref=w34] [bounds=175,1084,254,1104]:
  - /url: https://example.com/about
- textbox "邮箱" [clickable] [ref=w13] [bounds=32,340,408,381]:
  - /placeholder: example@domain.com
- textbox "用户名" [clickable] [ref=w11] [bounds=32,266,408,307]:
  - /placeholder: 请输入用户名
- textbox "密码" [clickable] [ref=w15] [bounds=32,414,408,455]:
  - /placeholder: 至少 8 位
- textbox "个人简介" [clickable] [ref=w19] [bounds=32,562,408,642]:
  - /placeholder: 一句话介绍自己
```

跨端一致性：Android 和 iOS 输出应**字节级一致**（除 bounds 数值）。

## 其他暂未输出的属性

按影响排序，本期 Issue 不一定全做，但记录下来作为后续 roadmap：

| 属性 | HTML | 用途 | Playwright 是否输出 | 建议 |
|---|---|---|---|---|
| `href` | `<a href>` | 链接目标 | ✅ 输出为 `url` prop | 本期做 |
| `placeholder` | `<input placeholder>` | 输入提示 | ✅ 输出为 `placeholder` prop | 本期做 |
| `input.type` | `<input type="password">` | 输入类型 | ❌ 不输出 | 暂不做 |
| `button.type` | `<button type="submit">` | 按钮行为 | ❌ 不输出 | 暂不做 |
| `name` | `<input name="user">` | 表单字段名 | ❌ 不输出 | 暂不做 |
| `id` | `<input id="user">` | 稳定锚点 | ❌ 不输出 | 暂不做 |
| `target="_blank"` | `<a target>` | 是否新开 tab | ❌ 不输出 | 暂不做 |
| `<title>` | 页面标题 | 顶层 context | ❌ ariaSnapshot 不输出（但 page API 单独提供） | 暂不做 |

## 关联

- 跨端 PoC：[[ios_poc_known_issues]] memory
- 设计参考：`docs/playwright-comparison-report.md`、`docs/llm-input-snapshot-standard.md`
- Android 实现：`web-plugin/src/main/assets/dom-serializer.js`
- iOS 实现：`ios/UIPerception/Capture/dom-serializer.js`（与 Android 共用）
- Playwright 参考：`/Users/caixiao/Workspace/projects/playwright/packages/injected/src/ariaSnapshot.ts` L185-193, L678-679
