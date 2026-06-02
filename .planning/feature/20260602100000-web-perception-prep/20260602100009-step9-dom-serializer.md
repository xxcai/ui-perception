# Step 9: WebDomSerializer — ARIA Tree JS 脚本

## 状态

已完成

## 文件

`web-plugin/src/main/java/com/hh/uiperception/webplugin/WebDomSerializer.java`

## Playwright 对应关系

每一步都标注了 Playwright 源码的具体位置、我们的简化点、以及简化的原因。

---

### 1. 隐藏标签过滤

- **Playwright**: `roleUtils.ts:306-308` `isElementHiddenForAria` — 直接跳过 STYLE/SCRIPT/NOSCRIPT/TEMPLATE
- **我们的实现**: 与 Playwright 一致，额外加了 HEAD/META/LINK（这些不会出现在 body 中但防御性过滤）
- **简化**: 无

---

### 2. 可见性检测 `isVisible()`

- **Playwright**: `roleUtils.ts:305` `isElementHiddenForAria` + `domUtils.ts:133` `isElementVisible`
- **保留**: display:none、visibility:hidden、opacity:0、零尺寸、aria-hidden=true 祖先链

#### 简化 2a: ~~跳过~~ 使用 `Element.checkVisibility()` API

- **Playwright 用它做什么**: roleUtils.ts:97-99，优先使用浏览器原生的 `Element.checkVisibility({checkOpacity:true, checkVisibilityCSS:true})` API
- **我们的实现**: 优先使用 checkVisibility API（Chrome 92+），不可用时回退到手动 CSS 检查。额外覆盖 `content-visibility: hidden` 等边缘场景
- **影响**: 与手动检查等价，并额外覆盖 content-visibility 等边缘 case

#### 简化 2b: 跳过 `display:contents` 子节点递归

- **Playwright 用它做什么**: roleUtils.ts:310-318，当元素自身是 `display:contents` 时（自身没有盒子，但子元素可见），需要递归检查是否**所有子元素**都不可见
- **为什么不需要它**: `display:contents` 在 Android WebView 页面中极少使用。它是 CSS 高级布局技巧（让 wrapper div 不产生盒子），常见于设计系统组件库。我们的目标页面是自有 app 内的 WebView，不是通用网页爬虫。即使遇到，最差情况是该元素被错误地标记为可见，不影响功能正确性
- **影响**: 极少数使用 `display:contents` 的自定义组件可能多输出一层空 generic 节点，后续 Java transform 的空节点剪枝可以处理

#### 简化 2c: 跳过 unslotted light DOM 检查

- **Playwright 用它做什么**: roleUtils.ts:336-337，当父元素有 shadowRoot 时，未分配到 slot 的 light DOM 子元素在视觉上不可见
- **为什么不需要它**: 这是 Web Components (Shadow DOM + slot) 的高级用法。自有 app 内的 WebView 页面几乎不会使用 Shadow DOM 和 slot 机制。Playwright 必须处理是因为它需要适配任意网页
- **影响**: 如果页面使用了 Web Components 且有 unslotted children，可能输出实际不可见的节点。Java transform 可过滤

---

### 3. Role 映射 `getRole()`

- **Playwright**: `roleUtils.ts:281` `getAriaRole` → `getExplicitAriaRole`(line 270) + `getImplicitAriaRole`(line 242)
- **保留**: 显式 role 属性优先 + tag→role 映射表（~20 个常见标签）

#### 简化 3a: 跳过 presentation/none role 继承

- **Playwright 用它做什么**: roleUtils.ts:247-259，当父元素有 `role="presentation"` 或 `role="none"` 时，特定子元素（如 LI 在 UL 内）也继承 presentation role，除非子元素有全局 ARIA 属性或可聚焦
- **为什么不需要它**: `role="presentation"` 是开发者有意隐藏元素语义的手段，在普通页面中很少使用。这是 WAI-ARIA 规范的边界情况，处理它需要检查全局 ARIA 属性列表（约 40 个属性）和 tabindex/focusable 状态，逻辑复杂度高但收益低
- **影响**: 使用了 `role="presentation"` 的页面中，某些元素可能输出真实 role 而非 generic。语义上更丰富，不是错误

#### 简化 3b: 跳过 landmark 嵌套检查

- **Playwright 用它做什么**: roleUtils.ts:155-180，`<header>` 和 `<footer>` 只在非 article/aside/main/nav/section 子级时才是 landmark role（banner/contentinfo），否则降级为 generic
- **为什么不需要它**: 这是一个可访问性规范的精确性要求。对于 LLM 理解页面结构，header 始终输出 `toolbar`、footer 始终输出 `section` 不会造成误导。准确区分的收益很小
- **影响**: 嵌套在 article 内的 header/footer 会输出 `toolbar`/`section` 而非 `generic`，语义上略有偏差但不影响 LLM 理解

#### 简化 3c: 跳过 select multiple/size 分支

- **Playwright 用它做什么**: roleUtils.ts:198-208，`<select multiple>` 或 `<select size>1` 映射为 `listbox`，否则映射为 `combobox`
- **为什么不需要它**: 我们统一映射为 `input`。native 侧没有 combobox/listbox 的区分（Android 的 Spinner 对应 input），保持一致。如果需要区分可以在 Java transform 根据 select 的 attributes 做更精确的判断
- **影响**: 所有 select 输出 `input`，丢失了 multiple 信息。可在后续迭代中补充

#### 简化 3d: 跳过 TH scope 和 form/section 命名条件

- **Playwright 用它做什么**: `<th>` 根据scope属性映射为 columnheader/rowheader；`<form>` 和 `<section>` 只有有 aria-label/aria-labelledby 时才映射为 form/region
- **为什么不需要它**: 我们把 table/tr 映射为 list/listitem（与 native 列表保持一致），不使用 table 语义角色。form 统一为 generic，section 统一为 section。简单统一，减少 LLM 需要理解的角色种类
- **影响**: 表格语义丢失（列头/行头区分），但列表语义足够 LLM 理解。form/section 条件判断的缺失影响极小

---

### 4. 可访问名称 `getName()`

- **Playwright**: `roleUtils.ts:504` `getElementAccessibleName` → `getTextAlternativeInternal`(line 622)
- **保留**: W3C accname spec 主路径（aria-label → aria-labelledby → alt/value/placeholder → textFromContent + ::before/::after → title）

#### 简化 4a: aria-labelledby 只取 textContent，不递归

- **Playwright 用它做什么**: roleUtils.ts:653-664，对 aria-labelledby 引用的每个元素递归调用 `getTextAlternativeInternal`，即被引用元素的名称也走完整的 accname 计算
- **为什么不需要它**: 递归计算增加了循环引用检测的复杂度（Playwright 用 visited Set 防 infinite loop）。绝大多数场景下 aria-labelledby 引用的元素就是一段静态文本，直接取 textContent 足够
- **影响**: 如果 aria-labelledby 引用的元素自身也有 aria-labelledby 或其他命名机制，我们的结果可能不准确。实际中极其罕见

#### ~~简化 4b~~ 已实现: embedded control substitution (accname step 2c)

- **Playwright 用它做什么**: roleUtils.ts:677-719，当元素包含表单控件时，用控件的 value 替代其文本内容作为名称
- **我们的实现**: `textFromContent(el)` 函数替代 `el.innerText`，遍历子节点时对 INPUT/SELECT/TEXTAREA 使用其 value 而非文本内容。例如 `<div>Price: <input value="99.9"> dollars</div>` → 名称 "Price: 99.9 dollars"
- **影响**: 包含嵌入式控件的元素名称现在包含控件实际值

#### 简化 4c: 跳过 label 关联查找

- **Playwright 用它做什么**: roleUtils.ts:730-912，通过 `for` 属性或父级 `<label>` 查找关联的 label 元素，将其文本作为 input 的名称
- **为什么不需要它**: 需要实现两种查找方式（`document.getElementById(for)` 和 `parentElement` 向上查找），还要排除 label 内的嵌入式控件文本。代码量中等，收益有限——因为我们的 baseline 测试页和大部分自有页面用 placeholder 提供名称，不依赖 label
- **影响**: 使用 `<label>` 但没有 placeholder/aria-label 的 input 可能输出空名称。可在后续迭代补充

#### ~~简化 4d~~ 已实现: CSS ::before/::after 伪元素内容

- **Playwright 用它做什么**: roleUtils.ts:944 的 `innerAccumulatedElementText` 中，通过 `getComputedStyle` 读取 `::before`/`::after` 的 content 属性
- **我们的实现**: `pseudoText(s)` 函数提取 CSS content 字符串值（支持双引号和单引号包裹），跳过 none/normal/函数调用（attr()/counter()/url()）。在 getName 的 "name from content" 步骤中，将 ::before 文本前置、::after 文本后置
- **影响**: CSS content 生成的文本（如装饰性引号、图标字符）会被捕获为名称的一部分

#### 简化 4e: 跳过 SVG title 子元素、fieldset legend、figure figcaption、table caption

- **Playwright 用它做什么**: roleUtils.ts:730-912 的多个特定分支
- **为什么不需要它**: 这些都是特定元素的命名约定。SVG 在 Android WebView 中不常见，fieldset/figure/table 在我们的目标页面中使用频率低。每个都需要几行特殊处理，收益不成比例
- **影响**: 这些特殊元素的名称可能为空，但它们的 role 仍然正确输出

---

### 5. Bounds `getBounds()`

- **Playwright**: `domUtils.ts` `computeBox`
- **无简化**: 直接使用 `getBoundingClientRect()`，与 Playwright 等价。Playwright 需要做 scroll offset 转换是因为它处理嵌套 iframe 的坐标系统一，我们不需要（iframe 坐标问题见 8c）

---

### 6. 状态提取 `getStates()`

- **Playwright**: `roleUtils.ts:1004-1099` 各 getAria* 函数

#### 简化 6a: 只读 DOM 属性，不读 aria-* attributes

- **Playwright 用它做什么**: 同时读取 DOM 属性（如 `el.disabled`）和 ARIA 属性（如 `el.getAttribute('aria-disabled')`），优先使用 ARIA 属性
- **为什么不需要它**: ARIA 属性是开发者手动设置的语义覆盖，在自有 app 的 WebView 页面中极少使用。DOM 属性是浏览器实际状态，更可靠。读取两个来源需要合并逻辑和优先级判断
- **影响**: 如果开发者设置了 `aria-disabled="true"` 但 DOM `disabled` 属性为 false，我们会输出没有 disabled 状态。实际中几乎不会出现这种不一致

#### 简化 6b: 跳过 fieldset 祖先 disabled 链

- **Playwright 用它做什么**: roleUtils.ts:1099，检查 disabled 的 `<fieldset>` 祖先，因为 HTML spec 中 fieldset 内的表单控件也会被禁用（除非在 `<legend>` 内）
- **为什么不需要它**: 需要向上遍历祖先链找 fieldset，还要处理 legend 豁免的特殊情况。disabled fieldset 在实际页面中很少使用
- **影响**: 在 disabled fieldset 内的表单控件不会被标记 disabled。可后续补充

#### 简化 6c: ~~跳过~~ 已实现 pressed 状态，保留 indeterminate

- **Playwright 用它做什么**: checkbox 的 indeterminate 状态（半选）、button 的 pressed 状态（切换按钮，通过 `aria-pressed` 属性）
- **indeterminate 保留**: 三态 checkbox 在真实 UI 中常见（文件管理器部分选中、邮件批量选择），且只需一行代码 `el.indeterminate`
- **pressed 已实现**: 读取 `aria-pressed` 属性，`"true"` 输出 `pressed`，`"mixed"` 输出 `pressed=mixed`
- **影响**: 三态 checkbox 和切换按钮的 pressed 状态均可正确捕获

---

### 7. Clickable 推断 `isClickable()`

- **Playwright**: 无直接对应。Playwright 的交互元素判断基于 `receivesPointerEvents`(roleUtils.ts:1149)，检查 CSS `pointer-events` 属性
- **我们的方案**: 基于元素类型（button/a/input/select）+ onclick handler + role=button 的简单推断
- **为什么这样设计**: clickable 的精确判断需要检查 CSS pointer-events、事件监听器（JS 无法检测 addEventListener 绑定的 handler）、 tabindex 等。在 capture 阶段做粗筛，transform 阶段可以根据 role + 上下文精确判断。Playwright 不在 JS 中做 clickable 判断——它在 render 阶段通过 ref 分配来隐式表达"这个元素可交互"
- **影响**: 某些通过 addEventListener 绑定点击的 div 不会被标记为 clickable（onclick 只检测 inline handler）。但这是所有基于静态分析的方案的共同限制

---

### 8. 主遍历 `serialize()`

- **Playwright**: `ariaSnapshot.ts:84` `generateAriaTree` → `visit()`(line 96)

#### 简化 8a: 跳过 aria-owns 补充子节点

- **Playwright 用它做什么**: ariaSnapshot.ts:129-136, 174-175，`aria-owns` 属性允许开发者将非子元素的节点声明为可访问性子节点。Playwright 会查找这些节点并添加到遍历列表
- **为什么不需要它**: `aria-owns` 是 ARIA 的重排机制，用于修复 DOM 结构与可访问性结构不一致的情况。它需要 `getElementById` 查找 + Set 防循环引用。在实际页面中极少使用
- **影响**: 使用了 `aria-owns` 的页面，元素的可访问性父子关系可能与视觉呈现不一致

#### 简化 8b: 跳过 slot assignedNodes 处理

- **Playwright 用它做什么**: ariaSnapshot.ts:159-161，当元素是 `<slot>` 时，用 `assignedNodes()` 获取被分配到该 slot 的实际节点来遍历
- **为什么不需要它**: 这是 Web Components (Shadow DOM slot) 的核心机制。自有 app 的 WebView 页面不会使用 Web Components。Playwright 必须支持是因为它需要适配使用 Lit/Stencil 等 Web Components 框架的页面
- **影响**: 使用 Web Components 的页面中，slot 内容不会被正确遍历

#### 简化 8c: 跳过 hasAssignedSlot 去重

- **Playwright 用它做什么**: ariaSnapshot.ts:165，跳过已经有 assignedSlot 的 light DOM 子节点，避免内容重复（因为 slot 机制会让这些内容出现在 slot 位置）
- **为什么不需要它**: 同上，依赖 Web Components 的 slot 机制
- **影响**: 无（与 8b 一起，不使用 Web Components 时无影响）

---

### 8a. Shadow DOM

- **Playwright**: `ariaSnapshot.ts:168-171` — 在 light DOM 之后遍历 shadowRoot
- **我们的实现**: 只遍历 shadowRoot 的 childNodes，不处理 slot
- **简化原因**: 同 8b/8c，slot 机制属于 Web Components 范畴

---

### 8c. Same-origin iframe

- **Playwright**: 不在 JS 内处理 iframe（通过 CDP 对每个 frame 独立执行 snapshot，不受同源限制）
- **我们的方案**: 通过 `contentDocument` 在 JS 内穿透同源 iframe
- **为什么这样设计**: 我们没有 CDP，只能依赖 JS 的 contentDocument 访问。同源策略限制跨域 iframe 访问，这是浏览器安全机制无法绕过。Playwright 通过 CDP 不受此限制
- **影响**: 跨域 iframe 内容丢失（已记录为已知限制）

---

### 9. 入口

- **Playwright**: `injectedScript.ts:313` `incrementalAriaSnapshot`，支持增量 diff（缓存上次 snapshot 对比）
- **我们的方案**: 每次全量，不支持增量
- **为什么不需要增量**: Playwright 的增量模式是为了减少每次 snapshot 的数据传输量（频繁轮询场景）。我们是手动触发（点击按钮或 HTTP 请求），不需要频繁 snapshot，全量输出更简单可靠
- **影响**: 无

---

## 简化分类总结

按简化原因分为三类：

**A. 适用场景差异**（Playwright 面向所有网页，我们面向自有 app）：
- display:contents、unslotted DOM、slot/assignedNodes、aria-owns、Web Components 相关
- 这些在自有 app 的 WebView 页面中几乎不会出现

**B. 可在 Java transform 阶段补充**（JS 保持简单，Java 做精确处理）：
- presentation 继承、landmark 嵌套、label 关联、fieldset disabled 链、aria-* attributes
- 这些不需要 DOM 访问，Java 侧有足够的数据做判断

**C. 收益不成比例**（复杂度高但实际影响极小，经评估后大部分不加）：
- ~~checkVisibility API~~：与手动检查重复 → **已实现**，优先使用原生 API，不可用时回退手动检查
- ~~embedded control substitution~~：40+ 行复杂逻辑 → **已实现**，textFromContent 替代 innerText
- ~~CSS ::before/::after~~：只有简单字符串场景易处理 → **已实现**，pseudoText 提取 CSS content 字符串
- ~~indeterminate 状态~~：一行代码，三态 checkbox 场景常见，**已加回**
- ~~pressed 状态~~：依赖 aria-pressed attribute → **已实现**，读取 aria-pressed (true/mixed)
