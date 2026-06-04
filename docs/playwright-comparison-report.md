# Playwright ARIA Snapshot Pipeline 逐项对比排查记录

> 生成日期: 2026-06-04
> 对照版本: Playwright `packages/injected/src/` (ariaSnapshot.ts, roleUtils.ts, domUtils.ts)
> 我方版本: `web-plugin/.../WebDomSerializer.java` + 相关 Java 类

---

## 排查方法

逐行阅读 Playwright 三个核心文件的每个函数，与我们的 `WebDomSerializer.java` 中的 JS 脚本对比，按处理阶段分类记录。

**Native 影响评估原则**：
- `SemanticRole` 枚举扩展：native 管道（`NativeRoleResolver`）不生成新 role → 无冲突
- JS 脚本修改：仅在 WebView 内执行 → 无影响
- `WebJsonParser` 修改：仅解析 JS 输出 → 无影响
- `TreeNormalizer` 修改：仅 web 管道使用 → 无影响
- `RefAssigner` / `SnapshotRenderer`：不在本次修改范围 → 无影响

---

## 一、入口与 DOM 遍历

### 1.1 入口函数

**Playwright** (`ariaSnapshot.ts:84-206 generateAriaTree`):
```
1. toInternalOptions(options) → 转换公开选项为内部选项
2. 创建 root AriaNode (fragment type)，computeBox(rootElement)
3. 定义 visit 闭包 (行 96-149)
4. 定义 processElement 闭包 (行 151-194)
5. beginAriaCaches() → visit(root, rootElement, true) → endAriaCaches()
6. normalizeStringChildren(root) → normalizeGenericRoles(root)
7. 返回 { root, elements, refs, iframeRefs }
```

**我们** (`WebDomSerializer.java:467-477`):
```
1. var root = {role:'screen', name:'', states:[], bounds:[], children:[]}
2. serialize(root, document.body, 0)
3. 返回 JSON.stringify({ url, title, root })
```

**差异**:
- Playwright 有缓存系统（10 个 Map），我们没有 → **不做**：性能优化，当前规模不需要
- Playwright 有 diff 支持（compareSnapshots/filterSnapshotDiff），我们没有 → **不做**：增量更新功能，我们每次全量捕获
- Playwright 返回 elements/refs 供后续查找使用，我们只返回树 → 设计差异

**Native 影响**: 无

---

### 1.2 visited Set 防环

**Playwright** (`ariaSnapshot.ts:97-99`):
```javascript
if (visited.has(node))
  return;
visited.add(node);
```

**我们**: 无 visited 追踪

**评估**: 标准 DOM 树无环。仅在 JS 手动创建循环引用时可能触发。Playwright 也是纯防御性代码。

**决策**: 不做

**Native 影响**: 无

---

### 1.3 文本节点处理

**Playwright** (`ariaSnapshot.ts:101-109`):
```javascript
// 行 101-103: 父不可见时跳过
if (!parentElementVisible)
  return;
// 行 105: textbox 内部文本不输出
// 行 108-109: 非 textbox → push 字符串子节点
ariaNode.children.push(value);
```

**我们** (`WebDomSerializer.java:355-361`):
```javascript
if (node.nodeType === 3) {
  var text = node.textContent.trim();
  if (!text || text.length === 0) return;
  if (container._isTextbox) return;  // textbox 内跳过
  container.children.push({role:'text', name:text, children:[]});
}
```

**差异**:
- Playwright 父不可见时跳过文本，我们没有这个检查
- Playwright 文本是纯字符串子节点，我们是 `{role:'text'}` 对象

**决策**: 保持现状。父不可见的文本节点本身也不应该出现在最终输出中（父节点不可见时子节点会 reattach，文本节点会在 reattach 过程中自然过滤）

**Native 影响**: 无

---

### 1.4 可见性模式

**Playwright** (`ariaSnapshot.ts:116-121`):
```javascript
// 三种模式:
// 'aria': 仅 ARIA 可见性
// 'ariaOrVisible': ARIA 可见 OR 视觉可见
// 'ariaAndVisible': ARIA 可见 AND 视觉可见
```

**我们**: 只有 ARIA 模式（等价 Playwright `'aria'` 模式）

**差异**: Playwright 的 AI 模式（`mode:'ai'`）使用 `ariaOrVisible`，即 aria 隐藏但视觉可见的元素仍输出。我们始终使用 aria 模式。

**决策**: 保持现状。我们的 agent 使用场景中，aria 模式已足够。

**Native 影响**: 无

---

### 1.5 不可见元素的子节点 reattach

**Playwright** (`ariaSnapshot.ts:123-148`):
```javascript
// 行 125-126: aria 模式下不可见 → return（不遍历子节点）
if (!parentElementVisible && options.visibility === 'aria')
  return;
// 行 128-148: 其他模式 → 创建或不创建 ariaNode，子节点输出到 childAriaNode || ariaNode
```

**我们** (`WebDomSerializer.java:376-414`):
```javascript
var visible = !isHiddenForAria(node);
var result = null;
if (visible) {
  // 创建 result 节点
}
var target = result || container;  // 不可见时 target = container（reattach）
```

**差异**: 我们始终 reattach 不可见元素的子节点到父容器。Playwright 在 `aria` 模式下直接丢弃。

**决策**: 保持现状。我们的 reattach 行为等价 Playwright 的 `ariaOrVisible` 模式，比 `aria` 模式更宽松，能捕获更多隐藏容器中的可见内容。

**Native 影响**: 无

---

### 1.6 aria-owns 收集

**Playwright** (`ariaSnapshot.ts:128-136`):
```javascript
// 收集 aria-owns 引用的元素作为额外子节点
const ariaChildren = [];
if (element.hasAttribute('aria-owns')) {
  const owned = getIdRefs(element, 'aria-owns');
  for (const child of owned) {
    if (child && !visited.has(child)) {
      ariaChildren.push(child);
    }
  }
}
```

**我们**: 未实现 aria-owns

**评估**: aria-owns 在移动端 WebView 中极少使用。实现需要处理：ID 引用解析、shadow DOM 作用域、循环引用检测。

**决策**: 不做

**Native 影响**: 无

---

### 1.7 block spacing + ::before/::after

**Playwright** (`ariaSnapshot.ts:151-194 processElement`):
```javascript
// 行 153-156: block 元素前加空格
if (isBlock)
  ariaNode.children.push(' ');
// 行 158: ::before
// 行 159-171: slot / child / shadow 遍历
// 行 174-175: aria-owns 子节点
// 行 177: ::after
// 行 179-180: block 元素后加空格
// 行 182-183: 唯一子节点等于 name → 清除
// 行 185-188: link href → prop
// 行 190-193: textbox placeholder → prop
```

**我们** (`WebDomSerializer.java:416-448`):
```javascript
// ::before 伪元素
if (result) {
  var before = pseudoText(getComputedStyle(node,'::before').content);
  if (before) result.children.push({role:'text', name:before, children:[]});
}
// shadow DOM 遍历
// light DOM 子节点遍历
// ::after 伪元素
```

**差异**:
- block spacing 在 processElement 中处理（子节点遍历前后加空格），我们在 `textFromContent` 中处理（name 计算时） → 等价
- Playwright 在子节点级别加空格字符串，我们在 name 计算级别加空格 → 略有差异但不影响最终输出
- Link href prop → **不做**：agent 不需要 URL
- Textbox placeholder prop → **不做**：我们作为 name fallback，是设计选择

**Native 影响**: 无

---

### 1.8 Shadow DOM slot 处理

**Playwright** (`ariaSnapshot.ts:159-171`):
```javascript
// SLOT 元素: 使用 assignedNodes()
if (element.tagName === 'SLOT') {
  const assigned = element.assignedNodes();
  for (const child of assigned) visit(...);
} else {
  // 普通元素: 遍历 firstChild/nextSibling
}
```

**我们** (`WebDomSerializer.java:426-441`):
```javascript
// shadow DOM
var shadow = node.shadowRoot;
if (shadow) {
  var sc = shadow.firstChild;
  while (sc) { serialize(target,sc,depth+1); sc = sc.nextSibling; }
}
// light DOM 子节点
var children = node.childNodes;
for (var i = 0; i < children.length; i++) { serialize(target,children[i],depth+1); }
```

**差异**: 我们不处理 slot 分配。Web Components slot 机制在移动端 WebView 中使用极少。

**决策**: 不做

**Native 影响**: 无

---

## 二、隐藏检测 (`isElementHiddenForAria`)

### 2.1 Hidden tag 过滤

**Playwright** (`roleUtils.ts:297-303`):
```javascript
function isElementIgnoredForAria(element) {
  const tagName = elementSafeTagName(element);
  return tagName === 'STYLE' || tagName === 'SCRIPT' || tagName === 'NOSCRIPT' || tagName === 'TEMPLATE';
}
```

**我们** (`WebDomSerializer.java:29-30`):
```javascript
var HIDDEN_TAGS = ['SCRIPT','STYLE','HEAD','META','LINK','NOSCRIPT','TEMPLATE'];
```

**差异**: 我们多了 HEAD/META/LINK。这些 tag 在 document.body 内不常见，多出来不影响。

**Native 影响**: 无

---

### 2.2 display:contents 透明处理

**Playwright** (`roleUtils.ts:309-318`):
```javascript
if (style.display === 'contents') {
  for (let child = element.firstChild; child; child = child.nextSibling) {
    if (child.nodeType === 1 && !isHiddenForAria(child)) return false;
    if (child.nodeType === 3) { /* 检查文本 */ }
  }
  return true;  // 所有子节点都不可见 → 元素不可见
}
```

**我们**: 完全相同的逻辑（`WebDomSerializer.java:45-51`）

**Native 影响**: 无

---

### 2.3 CSS 可见性检查

**Playwright** (`roleUtils.ts:321-325`):
```javascript
if (!isElementStyleVisibilityVisible(element))
  return true;
```

`isElementStyleVisibilityVisible` (`domUtils.ts:87-109`):
```javascript
// 优先 checkVisibility API
if (typeof element.checkVisibility === 'function') {
  return element.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true });
}
// Fallback: display/visibility/opacity
```

**我们** (`WebDomSerializer.java:54-60`):
```javascript
if (typeof el.checkVisibility === 'function') {
  if (!el.checkVisibility({checkOpacity:true,checkVisibilityCSS:true})) return true;
} else {
  if (s.display === 'none') return true;
  if (s.visibility === 'hidden') return true;
  if (parseFloat(s.opacity) === 0) return true;
}
```

**状态**: ✅ 已实现，等价

**Native 影响**: 无

---

### 2.4 祖先链检查

**Playwright** (`roleUtils.ts:328-354 belongsToDisplayNoneOrAriaHiddenOrNonSlotted`):
```javascript
// 递归检查祖先:
// 1. display:none → 隐藏
// 2. aria-hidden=true → 隐藏
// 3. 在 shadow root 中但非 slotted → 隐藏
```

**我们** (`WebDomSerializer.java:63-70`):
```javascript
var p = el.parentElement;
while (p) {
  var ps = getComputedStyle(p);
  if (ps.display === 'none') return true;
  if (p.getAttribute('aria-hidden') === 'true') return true;
  p = p.parentElement;
}
```

**差异**: 我们不检查 shadow DOM 中的 slot 分配状态。

**决策**: 不做 slot 检查（同 1.8 理由）

**Native 影响**: 无

---

## 三、Role 映射 (`getAriaRole`)

### 3.1 显式 Role 获取

**Playwright** (`roleUtils.ts:270-274 getExplicitAriaRole`):
```javascript
function getExplicitAriaRole(element) {
  const roles = element.getAttribute('role')?.trim() ?? '';
  // 取第一个合法 token
  for (const role of roles.split(/\s+/)) {
    if (validRoles.includes(role))  // validRoles: 所有 WAI-ARIA 合法 role 列表
      return role;
  }
  return null;
}
```

**我们** (`WebDomSerializer.java:91-94`):
```javascript
var explicit = el.getAttribute('role');
if (explicit === 'presentation' || explicit === 'none') return null;
if (explicit) return explicit;
```

**差异**:
1. 我们不验证 role 是否合法（`role="foobar"` 会直接使用）
2. 我们不处理多 token（`role="button menuitem"` 会原样输出）
3. 我们只特殊处理 presentation/none，不处理其他无效 role

**决策**: **做** — 增加合法性验证和首 token 提取

**Native 影响**: 无（纯 JS 修改）

---

### 3.2 presentation/none 冲突解决

**Playwright** (`roleUtils.ts:276-279 hasPresentationConflictResolution`):
```javascript
function hasPresentationConflictResolution(element, role) {
  // 有 global ARIA 属性（aria-label, aria-labelledby 等）或可聚焦 → 冲突解决
  // 此时 presentation/none 不生效，恢复 implicit role
  return hasGlobalAriaAttribute(element, role) || isFocusable(element);
}
```

**Playwright** (`roleUtils.ts:281 getAriaRole`):
```javascript
function getAriaRole(element) {
  const explicit = getExplicitAriaRole(element);
  if (explicit === 'presentation' || explicit === 'none') {
    if (hasPresentationConflictResolution(element, explicit))
      return getImplicitAriaRole(element);  // 恢复 implicit role
    return null;  // presentation 生效
  }
  if (explicit) return explicit;
  return getImplicitAriaRole(element);
}
```

**我们**:
```javascript
if (explicit === 'presentation' || explicit === 'none') return null;  // 无条件跳过
```

**差异**: `<button role="presentation" aria-label="Submit">` 在 Playwright 中仍识别为 button（因为 aria-label 冲突解决），我们直接跳过。

**决策**: **做** — 增加 global ARIA 检查和 focusable 检查

**实现**: 需要一个 `hasGlobalAriaAttribute(el)` 函数检查常见 global ARIA 属性

**Native 影响**: 无

---

### 3.3 presentation/none 继承

**Playwright** (`roleUtils.ts:229-240 kPresentationInheritanceParents` + `roleUtils.ts:242-268 getImplicitAriaRole`):
```javascript
// 子元素继承父元素的 presentation/none role
// 除非子元素有 global ARIA 属性或可聚焦
// 例如: <table role="presentation"><tr><td> → td 也变成 presentation
const kPresentationInheritanceParents = {
  // 子 tag → 父 tag 映射
  TD: ['TABLE'], TH: ['TABLE'], TR: ['TABLE'], THEAD: ['TABLE'],
  TBODY: ['TABLE'], TFOOT: ['TABLE'], CAPTION: ['TABLE'],
  LI: ['UL', 'OL'], OPTION: ['SELECT', 'DATALIST', 'OPTGROUP'],
  // ...
};
```

**我们**: 未实现继承

**评估**: 规则复杂（需维护父子 tag 映射表，子元素需检查 global ARIA / focusable 中断条件），实际场景有限（主要用于 layout table）。

**决策**: 不做

**Native 影响**: 无

---

### 3.4 隐式 Role — 缺失 Tag 详细对比

以下逐个 tag 对比 Playwright 映射与我们的行为：

#### 映射到已有 SemanticRole 的 tag

| Tag | Playwright 源码 | Playwright Role | 我们的当前行为 | 修改方案 |
|-----|----------------|----------------|-------------|---------|
| `AREA[href]` | roleUtils.ts:91 kImplicitRoleByTagName | link | generic | `case 'AREA': return el.hasAttribute('href') ? 'link' : 'generic';` |
| `DATALIST` | roleUtils.ts:155 | listbox | generic | `case 'DATALIST': return 'listbox';` |
| `DETAILS` | roleUtils.ts:165 | group | generic | `case 'DETAILS': return 'group';` |
| `DIALOG[open]` | roleUtils.ts:167-170 | dialog (open) / generic (closed) | generic | `case 'DIALOG': return el.hasAttribute('open') ? 'dialog' : null;` |
| `DL` | roleUtils.ts:173-178 | list | generic | `case 'DL': return 'list';` |
| `MENU` | roleUtils.ts:188 | list | generic | `case 'MENU': return 'list';` |
| `PROGRESS` | roleUtils.ts:197 | progress | generic | `case 'PROGRESS': return 'progress';` |

#### 需要新增 SemanticRole 的 tag

| Tag | Playwright 源码 | Playwright Role | 新增枚举 | 修改方案 |
|-----|----------------|----------------|---------|---------|
| `ARTICLE` | roleUtils.ts:91 | article | `ARTICLE("article")` | `case 'ARTICLE': return 'article';` |
| `ASIDE` | roleUtils.ts:91 | complementary | `COMPLEMENTARY("complementary")` | `case 'ASIDE': return 'complementary';` |
| `BLOCKQUOTE` | roleUtils.ts:91 | blockquote | `BLOCKQUOTE("blockquote")` | `case 'BLOCKQUOTE': return 'blockquote';` |
| `CAPTION` | roleUtils.ts:91 | caption | `CAPTION("caption")` | `case 'CAPTION': return 'caption';` |
| `DD` | roleUtils.ts:173 | definition | `DEFINITION("definition")` | `case 'DD': return 'definition';` |
| `DT` | roleUtils.ts:173 | term | `TERM("term")` | `case 'DT': return 'term';` |
| `HR` | roleUtils.ts:91 | separator | `SEPARATOR("separator")` | `case 'HR': return 'separator';` |
| `METER` | roleUtils.ts:91 | meter | `METER("meter")` | `case 'METER': return 'meter';` |
| `OPTGROUP` | roleUtils.ts:91 | group | `GROUP("group")` | `case 'OPTGROUP': return 'group';` |
| `OPTION` | roleUtils.ts:91 | option | `OPTION_ROLE("option")` | `case 'OPTION': return 'option';` |
| `OUTPUT` | roleUtils.ts:91 | status | `STATUS("status")` | `case 'OUTPUT': return 'status';` |
| `P` | roleUtils.ts:91 | paragraph | `PARAGRAPH("paragraph")` | `case 'P': return 'paragraph';` |
| `TFOOT/THEAD/TBODY` | roleUtils.ts:91 | rowgroup | `ROWGROUP("rowgroup")` | `case 'TFOOT':case 'THEAD':case 'TBODY': return 'rowgroup';` |

**Native 影响分析**:
- 新增 13 个 `SemanticRole` 枚举值
- `NativeRoleResolver` 基于 Android className 映射，不产生这些 role → **无冲突**
- `WebJsonParser.mapRole()` 自动匹配新 snapshotName → 无需修改
- `RefAssigner` 的 `isExecutableRole()` 集合不包含新 role → 新 role 不自动获得 ref，符合预期

---

### 3.5 TH/TD 上下文推断

**Playwright** (`roleUtils.ts:91 kImplicitRoleByTagName TABLE/TD/TH`):

```javascript
// TD: 检查祖先 table 的 role
TABLE: (element) => {
  // table role 取决于自身是否有 grid/treegrid 的显式 role
  // 默认: 'table'
}

TD: (element) => {
  const table = element.closest('table');
  const tableRole = table && getAriaRole(table);
  return (tableRole === 'grid' || tableRole === 'treegrid') ? 'gridcell' : 'cell';
}

TH: (element) => {
  // 分析同 row 内的 TH 位置
  const row = element.closest('tr');
  // 第一行的 TH → columnheader
  // 其他行的 TH → 根据 scope 属性或位置判断 → rowheader / columnheader
}
```

**我们** (`WebDomSerializer.java:154-157`):
```javascript
case 'TABLE': return 'table';
case 'TR': return 'row';
case 'TD': return 'cell';              // 无上下文检查
case 'TH': return 'columnheader';      // 始终 columnheader
```

**差异**:
1. TD 在 grid/treegrid 内应为 gridcell
2. TH 应根据位置/属性判断 columnheader vs rowheader

**决策**: **做** — 增加上下文推断

**实现复杂度**: 中等（需要向上查找 table ancestor，TH 需要 sibling 分析）

**Native 影响**: 无

---

### 3.6 Role 名称差异（我们的命名选择）

| Tag | Playwright Role | 我们的 Role | 评估 |
|-----|----------------|-----------|------|
| FOOTER | contentinfo | section | 功能等价，agent 通过 ref 交互，role 名不影响操作 |
| HEADER | banner | toolbar | 同上 |
| MAIN | main | screen | 同上 |
| IMG | img | image | 同上 |
| SECTION | region | section | 同上 |

**决策**: 不改。保持我们的命名。原因：
1. 这些 role 名称在 agent 交互中无实际差异（agent 通过 ref + 操作类型交互）
2. 改名会影响现有的 snapshot 输出格式，可能破坏下游消费者
3. SemanticRole 枚举已在使用中

**Native 影响**: 无（web 专用 role）

---

## 四、Name 计算 (`getElementAccessibleName`)

### 4.1 完整 accname spec 步骤对比

Playwright 实现 W3C Accessible Name Computation 规范的完整步骤 (`roleUtils.ts:622-942 getTextAlternativeInternal`)：

| Step | 规范步骤 | Playwright 行为 | 我们的行为 | 状态 |
|------|---------|----------------|-----------|------|
| 2a | aria-labelledby | 多 ID 空格分隔，递归 getTextAlternativeInternal | 单 ID，textContent.trim() | ⬜ 差异 |
| 2b | (被 2a 包含) | — | — | — |
| 2c | embedded control | input/select/textarea 值嵌入 label 文本 | 未实现 | ⬜ 差异 |
| 2d | aria-label | 直接取值 | 直接取值 | ✅ |
| 2e | native HTML naming | label[for], label 包裹, img alt, input value, table caption, figcaption, fieldset legend, details summary, placeholder | label[for], img alt, input value, placeholder | ⬜ 部分 |
| 2f | name from content | allowsNameFromContent 门控 + innerAccumulatedElementText | 无门控，textFromContent | ⬜ 差异 |
| 2i | title fallback | 最后 fallback | 最后 fallback | ✅ |

### 4.2 aria-labelledby 多 ID

**Playwright** (`roleUtils.ts:480-488 getAriaLabelledByElements`):
```javascript
function getAriaLabelledByElements(element) {
  return getIdRefs(element, 'aria-labelledby');
  // getIdRefs: 空格分割 ID 列表，逐个 getElementById
}
```

在 `getTextAlternativeInternal` 中 (`roleUtils.ts:653-670`):
```javascript
// 逐个 ref 元素递归计算 name
const refs = getAriaLabelledByElements(element);
for (const ref of refs) {
  const name = getTextAlternativeInternal(ref, {embeddedInLabelledBy: true});
  // 拼接
}
```

**我们** (`WebDomSerializer.java:226-230`):
```javascript
var lb = el.getAttribute('aria-labelledby');
if (lb) {
  var ref = document.getElementById(lb);  // 只取第一个匹配
  if (ref) { var t = ref.textContent.trim(); if (t) return t; }
}
```

**差异**:
1. 我们只取单个 ID（`document.getElementById(lb)` 会取 lb 字符串中的第一个 ID）
2. 我们不递归计算 ref 元素的 name（直接 textContent）

**决策**:
- 多 ID 拼接: **做** — `lb.split(/\s+/)` 逐个取
- 递归 name 计算: **不做** — 实现复杂，需处理循环引用、visited set、embedded control 等，textContent 已覆盖绝大多数场景

**Native 影响**: 无

---

### 4.3 label 包裹关联

**Playwright** (`roleUtils.ts:805-817`):
```javascript
// 通过 element.labels 属性获取关联的 label
// 包括 <label for="id"> 和 <label>包裹<input></label>
if (element.labels) {
  return getAccessibleNameFromAssociatedLabels(element.labels, options);
}
```

**我们** (`WebDomSerializer.java:232-237`):
```javascript
// 只处理 <label for="id">
var elId = el.id;
if (elId) {
  var lbl = document.querySelector('label[for="' + elId + '"]');
  if (lbl) { var lt = lbl.textContent.trim(); if (lt) return lt; }
}
```

**差异**: `<label><input>文本</label>` 这种包裹写法我们没有处理。

**决策**: **做** — 增加 `el.closest('label')` 检查

**实现**:
```javascript
// label 包裹关联
if (!result) {
  var parentLabel = el.closest('label');
  if (parentLabel) {
    var plt = parentLabel.textContent.trim();
    if (plt) return plt;
  }
}
```

**Native 影响**: 无

---

### 4.4 embedded control substitution

**Playwright** (`roleUtils.ts:677-719 getTextAlternativeInternal step 2c`):
```javascript
// 在 aria-labelledby 引用的元素中，如果包含 input/select/textarea
// 则用其值替代文本
// 例如: <span id="lbl"><input value="John"> 的信息</span>
// → "John 的信息"
```

**评估**: 实现复杂（需要递归遍历 ref 元素的子节点，替换 embedded control 的值），且场景有限（aria-labelledby 引用含表单控件）。

**决策**: 不做

**Native 影响**: 无

---

### 4.5 Native HTML naming 补充

**Playwright** (`roleUtils.ts:730-912`):

| 元素 | Playwright | 我们 | 决策 |
|------|-----------|------|------|
| TABLE → caption | `element.querySelector('caption')` | 未实现 | **做** |
| FIGURE → figcaption | `element.querySelector('figcaption')` | 未实现 | **做** |
| FIELDSET → legend | `element.querySelector('legend')` | 未实现 | **做** |
| DETAILS → summary | `element.querySelector('summary')` | 未实现 | **做** |

**实现模式一致**:
```javascript
if (tag === 'TABLE') {
  var caption = el.querySelector('caption');
  if (caption) { var ct = caption.textContent.trim(); if (ct) return ct; }
}
// FIGURE/FIELDSET/DETAILS 同理
```

**Native 影响**: 无

---

### 4.6 allowsNameFromContent 门控

**Playwright** (`roleUtils.ts:491-502 allowsNameFromContent`):
```javascript
function allowsNameFromContent(role, targetDescendant) {
  // 检查 role 是否允许从内容获取 name
  // 某些 role（如 textbox, checkbox, radio, img）不应从内容获取 name
  const nameFromContent = alwaysNameFromContent.includes(role)
    || (nameFromContentAsDescendant.includes(role) && targetDescendant);
  return nameFromContent;
}
```

**我们**: 无门控，所有元素都调用 textFromContent

**差异**: `<input type="text">hello</input>` 在 Playwright 中不会取 "hello" 作为 name（textbox 不允许 name from content），我们可能会取。

**决策**: **做** — 增加 allowsNameFromContent 检查

**实现**: 在 `getName()` 中 `textFromContent` 调用前检查 role 是否在允许列表中

**Native 影响**: 无

---

### 4.7 innerAccumulatedElementText — block spacing + CSS content

**Playwright** (`roleUtils.ts:944-990 innerAccumulatedElementText`):
```javascript
// block 元素前后加空格
var isBlock = tag !== 'BR' && getComputedStyle(child).display !== 'inline';
if (isBlock && result.length) result += ' ';
// ... 收集文本
if (isBlock) result += ' ';
// CSS ::before/::after
const beforeContent = getCSSContent(child, ':before');
const afterContent = getCSSContent(child, ':after');
```

**我们** (`WebDomSerializer.java:175-205 textFromContent`):
```javascript
var isBlock = cs.display !== 'inline';
if (isBlock && parts.length > 0 && parts[parts.length-1] !== ' ') parts.push(' ');
// ... 收集文本
if (isBlock) parts.push(' ');
```

**差异**: 基本等价。Playwright 还处理了 SVG title、slot 分配，我们跳过了这些。

**Native 影响**: 无

---

### 4.8 CSS content 属性解析

**Playwright** (`roleUtils.ts:399-477 getCSSContent / parseCSSContentPropertyAsString`):
```javascript
// 完整 CSS content 属性解析:
// 1. 字符串: "text" → text
// 2. attr(): attr(data-label) → 取属性值
// 3. alt 文本: "text" / "alt text" → 取 alt 部分
// 4. counter/name/uri → 忽略
```

**我们** (`WebDomSerializer.java:166-171 pseudoText`):
```javascript
// 简单解析: 只处理引号包裹的字符串
function pseudoText(s) {
  if (!s || s === 'none' || s === 'normal') return '';
  var c = s.charCodeAt(0);
  if ((c === 34 || c === 39) && s.charCodeAt(s.length-1) === c)
    return s.substring(1, s.length-1);
  return '';
}
```

**差异**: 我们不支持 attr()、alt 文本。这些是 CSS content 的高级用法。

**决策**: 保持现状。CSS content 中使用 attr() 和 alt 文本的情况极少。

**Native 影响**: 无

---

## 五、状态提取 (`toAriaNode` + `getAriaXxx`)

### 5.1 checked 状态

**Playwright** (`roleUtils.ts:1004-1037`):
```javascript
function getAriaChecked(element) {
  return getChecked(element, /* allowMixed */ true);
}

function getChecked(element, allowMixed) {
  // 1. Native: input[checkbox/radio].checked
  // 2. Native: input.indeterminate → 'mixed'
  // 3. aria-checked on valid roles → true/false/'mixed'
  const result = getAriaBoolean(element.getAttribute('aria-checked'));
  // role-gated: kAriaCheckedRoles
}
```

**我们** (`WebDomSerializer.java:283-288`):
```javascript
if (el.checked) {
  if ('checkbox,radio,...'.indexOf(role) >= 0) {
    s.push('checked');
  }
}
if (el.indeterminate) s.push('indeterminate');
```

**差异**:
1. 我们只检查 `el.checked`，不检查 `aria-checked` 属性
2. indeterminate 没有 role-gating

**决策**: **做** — 增加 `aria-checked` 检查（在前端框架中，某些元素用 aria-checked 而非 native checked）

**Native 影响**: 无

---

### 5.2 disabled 状态

**Playwright** (`roleUtils.ts:1099-1121`):
```javascript
function getAriaDisabled(element) {
  // 1. Native disabled (input/button/select/textarea/optgroup/option)
  // 2. belongsToDisabledFieldSet (fieldset[disabled] 继承)
  // 3. aria-disabled 祖先继承
  return isNativelyDisabled(element)
    || belongsToDisabledFieldSet(element)
    || hasExplicitAriaDisabled(element);
}

// kAriaDisabledRoles: 约 35 个 role 的 role-gating 列表
```

**我们** (`WebDomSerializer.java:290`):
```javascript
if (el.disabled) s.push('disabled');
```

**差异**:
1. 无 role-gating
2. 无 aria-disabled
3. 无 fieldset disabled 继承
4. 无 aria-disabled 祖先继承

**决策**: **做** — 全部补上

**实现**:
```javascript
// disabled role-gating
var disabledRoles = 'application,button,composite,...';
if (isDisabled(el) && disabledRoles.indexOf(role) >= 0) s.push('disabled');

// isDisabled 函数:
// 1. el.disabled (native)
// 2. 祖先 fieldset[disabled]
// 3. el.getAttribute('aria-disabled') === 'true' 或祖先链
```

**Native 影响**: 无

---

### 5.3 expanded 状态

**Playwright** (`roleUtils.ts:1066-1080`):
```javascript
function getAriaExpanded(element) {
  // 1. Native: DETAILS.open
  if (element.tagName === 'DETAILS')
    return element.open;
  // 2. aria-expanded on valid roles
  const result = getAriaBoolean(element.getAttribute('aria-expanded'));
  // kAriaExpandedRoles: application, button, checkbox, combobox, gridcell,
  //   link, listbox, menuitem, row, rowheader, tab, treeitem,
  //   columnheader, menuitemcheckbox, menuitemradio, switch
}
```

**我们** (`WebDomSerializer.java:292-297`):
```javascript
var expanded = el.getAttribute('aria-expanded');
if (expanded) {
  if ('application,button,combobox,listbox,...'.indexOf(role) >= 0) {
    s.push(expanded === 'true' ? 'expanded' : 'collapsed');
  }
}
```

**差异**:
1. 缺少 DETAILS.open native 支持
2. role 列表缺少: checkbox, gridcell, link

**决策**: **做** — 补全

**Native 影响**: 无

---

### 5.4 selected 状态

**Playwright** (`roleUtils.ts:993-1001`):
```javascript
function getAriaSelected(element) {
  // 1. Native: OPTION.selected
  if (element.tagName === 'OPTION')
    return element.selected;
  // 2. aria-selected on valid roles
  return getAriaBoolean(element.getAttribute('aria-selected'));
  // kAriaSelectedRoles: gridcell, option, row, tab, rowheader, columnheader, treeitem
}
```

**我们** (`WebDomSerializer.java:305-310`):
```javascript
var selected = el.getAttribute('aria-selected');
if (selected === 'true') {
  if ('gridcell,option,...'.indexOf(role) >= 0) {
    s.push('selected');
  }
}
```

**差异**: 不检查 native `OPTION.selected`

**决策**: **做** — 增加 `el.selected` 检查（WeLink 已发现的选中状态检测缺失问题）

**Native 影响**: 无

---

### 5.5 readonly 状态

**Playwright** (`roleUtils.ts:1039-1050`):
```javascript
function getReadonly(element) {
  // aria-readonly on valid roles
  // kAriaReadonlyRoles: checkbox, combobox, grid, gridcell, listbox,
  //   radiogroup, slider, spinbutton, textbox, columnheader, rowheader,
  //   searchbox, switch, treegrid
}
```

**我们**: 未实现

**决策**: **做** — 增加 readonly 状态提取

**Native 影响**: 无

---

### 5.6 invalid 状态

**Playwright** (`roleUtils.ts:562-580`):
```javascript
function getAriaInvalid(element) {
  // aria-invalid: 'false', 'true', 'grammar', 'spelling'
}

function getValidityInvalid(element) {
  // HTML5: element.validity.valid === false
}
```

**我们**: 未实现

**决策**: **做** — 增加 invalid 状态（aria-invalid + HTML5 validity）

**Native 影响**: 无

---

## 六、树标准化

### 6.1 normalizeStringChildren

**Playwright** (`ariaSnapshot.ts:313-341`):
```javascript
// 1. 连续字符串子节点合并（join with normalizeWhiteSpace）
// 2. 唯一子节点等于 name → 清除
```

**我们** (`TreeNormalizer.java:84-117 mergeConsecutiveTexts`):
```java
// 1. 连续 text 子节点合并（join with " "）
// 2. 唯一子节点 name 去重
```

**状态**: ✅ 等价

**Native 影响**: TreeNormalizer 仅 web 管道使用，无影响

---

### 6.2 normalizeGenericRoles

**Playwright** (`ariaSnapshot.ts:290-311`):
```javascript
// generic + 无 name + 至多 1 个子节点 → 解包
// 条件: 子节点有 ref 时也解包（这点与我们有差异）
```

**我们** (`TreeNormalizer.java:42-48`):
```java
// generic + 无 name + 无 states + 无 ref + 至多 1 个子节点 → 解包
```

**差异**: 我们要求无 ref 才解包（保留有 ref 的 generic 节点），Playwright 解包时不管 ref。这是有意设计——保留有 ref 的节点以便 agent 定位。

**决策**: 保持现状

**Native 影响**: 无

---

### 6.3 inline generic + 单文本子节点扁平化

**Playwright** (`ariaSnapshot.ts:249`):
```javascript
// role === 'generic' && inline && 单文本子节点 → 不创建节点
```

**我们** (`WebDomSerializer.java:385-388`):
```javascript
if (role === 'generic') {
  var cs = getComputedStyle(node);
  if (cs.display === 'inline' && node.childNodes.length === 1
      && node.childNodes[0].nodeType === 3) return;
}
```

**状态**: ✅ 等价

**Native 影响**: 无

---

## 七、Ref 分配

### 7.1 computeAriaRef

**Playwright** (`ariaSnapshot.ts:208-221`):
```javascript
// 1. 只对 interactable 元素分配 ref（receivesPointerEvents）
// 2. 缓存在 element._ariaRef，role/name 不变则复用
// 3. 格式: refPrefix + 'e' + (++lastRef)
```

**我们**: Java 侧 RefAssigner，web 模式下所有有 bounds 的元素都考虑分配 ref

**差异**: Playwright 通过 `receivesPointerEvents` 检查 pointer-events CSS 属性，我们通过 `isClickable` 启发式推断。

**决策**: 保持现状。我们的 clickable 推断已足够，且 RefAssigner 的过滤逻辑已经成熟。

**Native 影响**: 无

---

### 7.2 receivesPointerEvents

**Playwright** (`roleUtils.ts:1149-1193`):
```javascript
// 递归检查元素及祖先的 pointer-events CSS 属性
// pointer-events: none → 不接收指针事件 → 不分配 ref
```

**我们**: 未实现。由 `isClickable` JS 函数 + Java RefAssigner 共同决定。

**决策**: 不做。当前 clickable 推断已覆盖常见场景。

**Native 影响**: 无

---

## 八、不做项汇总

| # | 项目 | 不做原因 | 如需做，复杂度 |
|---|------|---------|-------------|
| 2 | visited Set 防环 | DOM 无环，防御性代码 | 低 |
| 9 | aria-owns | 移动端极少，实现复杂 | 高 |
| 18 | Shadow DOM slot 隐藏 | Web Components 移动端极少 | 高 |
| 22 | presentation 继承 | 规则复杂，影响极小 | 中 |
| 61 | MATH → math | MathML 移动端极少 | 低（仅加映射） |
| 83 | TIME → time | 纯语义标签，无交互价值 | 低（仅加映射） |
| 85 | aria-labelledby 递归 | 实现复杂，textContent 已覆盖 | 高 |
| 86 | embedded control | 实现复杂，影响面大 | 高 |
| 105 | SVG title | 移动端 SVG 有限 | 低 |
| 106 | Slot 文本 | 依赖 Web Components API | 中 |
| 139 | Link href prop | agent 不需要，渲染不支持 prop | 低 |
| 140 | Placeholder prop | 设计选择（作为 name fallback） | 低 |
| 145 | 缓存系统 | 性能优化，当前不需要 | 高 |
| 7.2 | receivesPointerEvents | clickable 推断已足够 | 中 |
| 3.6 | Role 名称统一 | 现有命名不影响 agent 操作 | 中（需改枚举） |
| 4.8 | CSS content 高级解析 | attr()/alt 极少使用 | 中 |
