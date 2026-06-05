# LLM Input Snapshot v1

## 1. Pipeline 概览

### 1.1 Native Pipeline

```
Activity DecorView
       │
       ▼
┌──────────────────────────────────────────────────────────────────┐
│  ViewHierarchyDumper  (UI 线程, in-process)                      │
│                                                                  │
│  ┌─ 1. 节点过滤 ──────────────────────────────────────────────┐  │
│  │  isMeaningful(): visibility!=VISIBLE / width<=0 / height<=0│  │
│  │  alpha<=0 / 无父节点且非根视图 → 跳过                       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                           ▼                                      │
│  ┌─ 2. 基础属性提取 ──────────────────────────────────────────┐  │
│  │  className / resource-id / text(TextView) / hint(TextView) │  │
│  │  contentDescription / bounds(getLocationOnScreen)          │  │
│  │  enabled / focusable / selected / checked(Checkable)       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                           ▼                                      │
│  ┌─ 3. 可点击性检测 (三级信号) ──────────────────────────────┐  │
│  │  view.isClickable()                                         │  │
│  │  view.hasOnClickListeners()                                 │  │
│  │  反射 AdapterView.mOnItemClickListener                     │  │
│  │  反射 RecyclerView.mOnItemTouchListeners                   │  │
│  │  反射检测 onTouchEvent 重写 (vs View/ViewGroup 基类)       │  │
│  └────────────────────────────────────────────────────────────┘  │
│                           ▼                                      │
│  ┌─ 4. 输出 XML ─────────────────────────────────────────────┐  │
│  │  <node class="..." clickable="..." overrides-onTouchEvent  │  │
│  │        has-onclick-listener="..." has-item-click-listener  │  │
│  │        bounds="..." ...> 子节点递归 </node>                 │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│  NativeViewXmlParser │  XML → NativeViewNode 树
└──────────┬───────────┘
           ▼
┌──────────────────────────────────────────────────────────────────┐
│  NativeRoleResolver                                              │
│                                                                  │
│  ┌─ resolveByClass ──────────────────────────────────────────┐   │
│  │  className 简名匹配 (~20 模式)                             │   │
│  │  DecorView→screen / WebView→webview / Toolbar→toolbar      │   │
│  │  RecyclerView→list / EditText→input / Button→button / ...  │   │
│  └───────────────────────────────────────────────────────────┘   │
│                           ▼                                      │
│  ┌─ adjustByAttributes ──────────────────────────────────────┐   │
│  │  clickable/longClickable/onTouchEvent override             │   │
│  │    + generic/text/image → button                           │   │
│  │  scrollable + generic → scroll                             │   │
│  │  editable + text/generic → input                           │   │
│  └───────────────────────────────────────────────────────────┘   │
│                           ▼                                      │
│  ┌─ resolveName ─────────────────────────────────────────────┐   │
│  │  text → contentDescription                                 │   │
│  │  INPUT: hint → contentDesc → resourceId                    │   │
│  │  其他: resourceId (下划线→空格, 去包名)                     │   │
│  └───────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│  NativeSemanticTreeBuilder                                       │
│                                                                  │
│  ┌─ 1. 集合子项提升 ────────────────────────────────────────┐   │
│  │  LIST/GRID 子节点: generic/card/section → listitem          │   │
│  │  clickable 的 generic (attribute:clickable) → listitem       │   │
│  └───────────────────────────────────────────────────────────┘   │
│                           ▼                                      │
│  ┌─ 2. 状态附加 ─────────────────────────────────────────────┐   │
│  │  disabled / checked / selected / focused / scrollable       │   │
│  │  password / value=(INPUT的text)                              │   │
│  └───────────────────────────────────────────────────────────┘   │
│                           ▼                                      │
│  ┌─ 3. Clickable 状态 (仅 LIST_ITEM) ────────────────────────┐   │
│  │  clickable / hasOnClickListener / hasTouchOverride          │   │
│  │    → "clickable"                                             │   │
│  │  父容器 itemClick/itemTouch listener → "clickable=inferred" │   │
│  │  以上都没有 → "clickable=guessed"                            │   │
│  └───────────────────────────────────────────────────────────┘   │
│                           ▼                                      │
│  ┌─ 4. Generic 折叠 ─────────────────────────────────────────┐   │
│  │  generic 节点: 无 name / 无 state / 无 ref / 仅一个子节点   │   │
│  │  → 用子节点替换自身                                          │   │
│  └───────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│  RefAssigner (native)                                            │
│                                                                  │
│  ┌─ ref 分配规则 ────────────────────────────────────────────┐   │
│  │  仅可交互角色: button/input/checkbox/radio/switch/slider   │   │
│  │  picker/list/grid/scroll/webview                            │   │
│  │  LIST_ITEM: 三级 clickable 逻辑                             │   │
│  │    clickable/inferred → 分配                                │   │
│  │    guessed 且无 executable 子节点 → 分配                    │   │
│  │    否则 → 不分配                                            │   │
│  │  前缀: "n" (n1, n2, ...)                                    │   │
│  └───────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│  SnapshotRenderer    │  SemanticNode 树 → YAML 文本
└──────────────────────┘
```

### 1.2 Web Pipeline

```
WebView DOM
       │
       ▼
┌──────────────────────────────────────────────────────────────────┐
│  dom-serializer.js  (IIFE, 注入 WebView 执行)                    │
│                                                                  │
│  ┌─ 1. 可见性过滤 ──────────────────────────────────────────────┐ │
│  │  isHiddenForAria(): hidden tag / display:none / visibility   │ │
│  │  opacity:0 / aria-hidden / 祖先链传播 / display:contents    │ │
│  │  隐藏父节点的可见子节点 → 重新挂载到父容器                    │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 2. Role 映射 ──────────────────────────────────────────────┐ │
│  │  getRole(): explicit role attr → getImplicitRole(tag)        │ │
│  │  implicit: HTML-AAM 标签映射 (~50 tags)                      │ │
│  │  INPUT 按 type 分派 (checkbox/radio/button/spinbox/...)      │ │
│  │  IMG alt="" → 排除; SVG → image                              │ │
│  │  presentation/none 冲突: 有全局 ARIA 或 tabindex → implicit  │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 3. Name 计算 ──────────────────────────────────────────────┐ │
│  │  getName(): aria-label → aria-labelledby → label[for]/包裹   │ │
│  │  → HTML 原生 (img[alt]/input[value]/textarea[placeholder])   │ │
│  │  → 容器子元素 (table>caption/figure>figcaption/svg>title)     │ │
│  │  → nameFromContent (role-gated) + ::before/::after           │ │
│  │  → title 回退                                                │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 4. Bounds 提取 ────────────────────────────────────────────┐ │
│  │  getBoundingClientRect() → [left, top, right, bottom]        │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 5. State 提取 ─────────────────────────────────────────────┐ │
│  │  getStates(): checked/disabled/expanded/pressed/selected     │ │
│  │  readonly/invalid/focused/level (均 role-gated)              │ │
│  │  disabled 含 fieldset 继承; expanded 含 <details>.open       │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 6. Clickable 推断 ─────────────────────────────────────────┐ │
│  │  BUTTON/A/SELECT/INPUT(非hidden) / role=button / onclick     │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 7. DOM 遍历 (serialize) ───────────────────────────────────┐ │
│  │  递归遍历: text节点 → role:text                              │ │
│  │  inline generic 单文本子节点 → 跳过，文本流入父容器           │ │
│  │  SVG <title> → 跳过 (仅贡献给 SVG 自身 name)                 │ │
│  │  Shadow DOM → shadowRoot 遍历                                │ │
│  │  ::before/::after → text 子节点                              │ │
│  │  同源 iframe → contentDocument 递归                          │ │
│  │  每个可见元素分配 __pr_idx (操作定位用)                       │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                           ▼                                      │
│  ┌─ 8. 输出 JSON ──────────────────────────────────────────────┐ │
│  │  { url, title, root: { role, name, states, bounds, children}}│ │
│  └──────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────┐
│  WebJsonParser       │  JSON → SemanticNode 树
│                      │  role 匹配 SemanticRole 枚举，未知 → GENERIC
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│  TreeNormalizer      │  合并连续 text / 折叠空 generic / 去除重复子文本
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│  RefAssigner (web)   │  所有有效 bounds 节点分配 ref=w1, w2, ...
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│  SnapshotRenderer    │  SemanticNode 树 → YAML 文本
└──────────────────────┘
```

Native 和 Web 从 RefAssigner 开始共享同一套代码，统一输出格式。

### 1.3 输出示例 (Native + Web 混合场景)

一个带有顶部 Toolbar + WebView 内容区 + 底部 Tab 的典型页面。Native 和 Web 分别独立抓取，webview 在 native 树中仅作占位符，web 内容以 `--- Web ---` 分隔符拼接在下方：

```yaml
- screen:
  - toolbar "HIS Mall":
    - button "Search" [ref=n1]
    - button "Cart" [ref=n2]
  - webview [ref=n3]
  - tabbar:
    - button "Home" [selected] [ref=n4]
    - button "Category" [ref=n5]
    - button "Me" [ref=n6]

--- Web ---

- screen:
  - heading "Flash Sale"
  - list:
    - listitem:
      - link "Wireless Earbuds" [clickable] [ref=w1] [bounds=10,200,350,320]
      - text "$29.99"
    - listitem:
      - link "Smart Watch" [clickable] [ref=w2] [bounds=10,330,350,450]
      - text "$49.99"
  - form:
    - textbox "Search products..." [ref=w3] [bounds=10,460,350,510]
    - button "Go" [clickable] [ref=w4] [bounds=360,460,400,510]
```

native 部分 ref 前缀 `n`，web 部分 ref 前缀 `w`；两棵树通过 `--- Web ---` 分隔。

---

## 2. Native / DroidRun / Web / Playwright 阶段对比

### 2.1 四条 Pipeline 流程图

```
 Native (本项目)                  DroidRun                   Web (本项目)              Playwright
═══════════════════════    ═════════════════════    ═══════════════════    ══════════════════════
┌─ ViewHierarchyDumper ─┐   Accessibility Service    JS 注入 DOM 遍历         JS 注入 DOM 遍历
│ 1.节点过滤             │   (系统服务)                (evaluateJavascript)     (CDP evaluate)
│ 2.基础属性提取         │        │                         │                       │
│ 3.可点击性检测(反射)    │        ▼                         ▼                       ▼
│ 4.输出XML              │   JSON a11y_tree             ┌── dom-serializer.js ──┐ AriaNode 树
└────────────────────────┘        │                      │ 1.可见性过滤           │      │
        │                         │                      │ 2.Role映射             │      ▼
        ▼                         │                      │ 3.Name计算             │ normalizeString
XmlParser →                        │                      │ 4.Bounds提取           │ Children +
NativeViewNode                     │                      │ 5.State提取            │ normalizeGeneric
        │                          │                      │ 6.Clickable推断        │ Roles
        ▼                          │                      │ 7.DOM遍历(Shadow/iframe)│     │
┌─ NativeRoleResolver ──┐          │                      │ 8.输出JSON             │      ▼
│ resolveByClass         │          │                      └────────────────────────┘ renderAriaTree
│ adjustByAttributes     │          ▼                         │                       │
│ resolveName            │   TreeFilter                      │                       │
└────────────────────────┘   (可见性/尺寸过滤)                 ▼                       │
        │                          │                   WebJsonParser                    │
        ▼                          │                  → SemanticNode                    │
┌─ NativeSemanticTree   ─┐          │                        │                        │
│ 1.集合子项提升          │          ▼                        ▼                        │
│ 2.状态附加              │   IndexedFormatter         TreeNormalizer                   │
│ 3.Clickable状态(三级)   │   (className截短作role)     (合并/折叠/去重)                  │
│ 4.Generic折叠           │          │                        │                        │
└────────────────────────┘          │                        │                        │
        │                           ▼                        ▼                        ▼
┌─ RefAssigner(native) ──┐   序号索引 (1,2,3...)        RefAssigner              ref=e1, e2, ...
│ 可交互角色+ListItem     │          │                        │
│ 三级clickable逻辑       │          │                        │
│ 前缀: "n"               │          │                        │
└────────────────────────┘          │                        │
        │                           ▼                        ▼                        ▼
SnapshotRenderer               格式化文本行            SnapshotRenderer         YAML 文本
→ YAML                         (非标准YAML)             → YAML                   → YAML
```

### 2.2 阶段差异表

| 阶段 | Native | DroidRun | Web | Playwright |
|---|---|---|---|---|
| **数据源** | 进程内 View 树遍历 (`DecorView`) | 系统 Accessibility Service | WebView JS 注入 | CDP evaluate JS 注入 |
| **原始格式** | XML | JSON (`a11y_tree`) | JSON | 内存中 AriaNode 树 |
| **可见性过滤** | 遍历时过滤 (visibility/size/alpha) | 后置 TreeFilter (可见面积阈值/尺寸) | JS 内 ARIA 可见性判断 | `isElementHiddenForAria` + `isElementVisible` 双轨 |
| **Role 确定** | class 名匹配 + 属性调整 (两步) | className 截短直接使用 (如 `Button`) | ARIA explicit → implicit by tag (HTML-AAM) | ARIA explicit → implicit by tag (HTML-AAM) |
| **Name 计算** | text → contentDesc → resourceId | text → contentDesc → resourceId → className | aria-label → labelledby → label → title → nameFromContent | aria-labelledby → aria-label → native HTML naming → nameFromContent → title |
| **State 提取** | disabled/checked/selected/focused/scrollable/password/clickable(三级) | 仅 isChecked 输出到文本 (其他在原始数据中存在) | checked/disabled/expanded/pressed/selected/readonly/invalid/focused/level/value | checked/disabled/expanded/pressed/selected/level (均 role-gated) |
| **Ref 策略** | 仅可交互节点 (`n1`, `n2`, ...) | 全节点顺序索引 (`1`, `2`, ...) | 全有效 bounds 节点 (`w1`, `w2`, ...) | 全可聚焦/可交互节点 (`e1`, `e2`, ...) |
| **树优化** | 集合子项提升 / generic 单子节点折叠 | 无 | 连续 text 合并 / 空 generic 折叠 / 重复子文本去除 | 连续 text 合并 / generic 折叠 / name 重复子文本去除 |
| **输出格式** | YAML (缩进+连字符) | 自定义文本行 (非 YAML) | YAML (缩进+连字符) | YAML (缩进+连字符) |
| **运行位置** | 设备 UI 线程 | 设备端 Portal → Python 后端 | 设备 UI 线程 (JS) + Java | 浏览器内 JS + Node 服务端 |

---

## 3. Native vs DroidRun：原始数据抓取对比

### 3.1 数据采集层

| 对比维度 | Native (本项目) | DroidRun |
|---|---|---|
| **采集方式** | 进程内 View 树遍历，反射读取 View 属性 | 系统 Accessibility Service，通过 Portal TCP/ContentProvider 传输 |
| **入口** | `activity.getWindow().getDecorView()` | `DroidrunAccessibilityService` (系统级无障碍服务) |
| **窗口范围** | 仅 Activity 主窗口 | 所有可访问窗口 |
| **运行线程** | 必须在 UI 线程 | 后台 Service，通过 HTTP 获取 |
| **是否需要无障碍权限** | 否 | 是 |

### 3.2 可见性过滤

| 对比维度 | Native (本项目) | DroidRun |
|---|---|---|
| **过滤时机** | 遍历时内联过滤 | 抓取后后置过滤 (TreeFilter) |
| **过滤条件** | `visibility != VISIBLE` / `width <= 0` / `height <= 0` / `alpha <= 0` | DetailedFilter: 可见面积 < 10%; ConciseFilter: 不与屏幕相交 or 尺寸 < 5px |
| **键盘元素** | 保留 | 过滤 Google 键盘元素 |

### 3.3 属性提取

| 属性 | Native (本项目) | DroidRun |
|---|---|---|
| class | `view.getClass().getName()` | AccessibilityNodeInfo `className` |
| text | `TextView.getText()` | `text` 字段 |
| hint | `TextView.getHint()` | 不提取 |
| contentDescription | `view.getContentDescription()` | `contentDescription` 字段 |
| resourceId | `view.getResources().getResourceName(id)` | `viewIdResourceName` 字段 |
| bounds | `getLocationOnScreen()` + `getWidth/getHeight` | `boundsInScreen` (AccessibilityNodeInfo 提供) |
| clickable | `view.isClickable()` | `isClickable` 字段 |
| onClickListener | `view.hasOnClickListeners()` (API) | 不检测 |
| itemClickListener | 反射 `AdapterView.mOnItemClickListener` | 不检测 |
| itemTouchListener | 反射 `RecyclerView.mOnItemTouchListeners` | 不检测 |
| onTouchEvent override | 反射检测方法声明类 | 不检测 |
| longClickable | XML 解析时支持 (Dumper 当前未输出) | `isLongClickable` 字段 |
| enabled | `view.isEnabled()` | `isEnabled` 字段 |
| checked | `Checkable.isChecked()` | `isChecked` 字段 |
| selected | `view.isSelected()` | `isSelected` 字段 |
| focused | `view.isFocusable()` | `isFocused` 字段 |
| scrollable | `instanceof ScrollView/ListView` | `isScrollable` 字段 |
| editable | 不提取 | `isEditable` 字段 (用于判断键盘状态) |

### 3.4 Role 确定

| 对比维度 | Native (本项目) | DroidRun |
|---|---|---|
| **策略** | 语义角色映射 (class 名 → SemanticRole 枚举) | className 截短直接输出 |
| **示例** | `androidx.recyclerview.widget.RecyclerView` → `list` | `androidx.recyclerview.widget.RecyclerView` → `RecyclerView` |
| **属性提升** | clickable 的 generic/text/image → `button`; scrollable generic → `scroll`; editable text → `input` | 无属性提升 |
| **回退** | 有 text/contentDesc → `text`; 否则 → `generic` | 直接用 className |

### 3.5 Name 计算

| 优先级 | Native (本项目) | DroidRun |
|---|---|---|
| 1 | text | text |
| 2 | contentDescription | contentDescription |
| 3 | INPUT 特殊: hint → contentDesc → resourceId | resourceId (含完整包名) |
| 4 | resourceId (可读化处理) | className (完整类名) |

### 3.6 Ref / 索引分配

| 对比维度 | Native (本项目) | DroidRun |
|---|---|---|
| **范围** | 仅可交互节点 (button/input/checkbox/switch/slider/picker/list/grid/scroll/webview) | 全部节点 |
| **格式** | `n1`, `n2`, ... (字符串) | `1`, `2`, ... (整数) |
| **List item 特殊逻辑** | 三级: clickable → clickable=inferred (父容器) → clickable=guessed | 无区分 |

### 3.7 输出格式

**Native (本项目)：**
```yaml
- listitem [clickable] [ref=n1]:
  - text "梁晓舟"
  - text "明天上班"
```

**DroidRun：**
```
1. TextView: 梁晓舟 - 10,20,200,50
2. TextView: 明天上班 - 10,50,200,80
```

---

## 4. Web vs Playwright：原始数据抓取对比

### 4.1 执行环境与入口

| 对比维度 | Web (本项目) | Playwright |
|---|---|---|
| **注入方式** | `WebView.evaluateJavascript()` | CDP `Runtime.evaluate` |
| **代码形式** | assets 中独立 JS 文件 (IIFE) | 内联注入脚本 (bundled) |
| **入口函数** | IIFE 直接执行，返回 JSON 字符串 | `incrementalAriaSnapshot()` → `generateAriaTree()` → `renderAriaTree()` |
| **iframe 处理** | JS 内递归处理同源 iframe | 浏览器内记录 ref，服务端递归获取每个 iframe 内容后拼接 |
| **Shadow DOM** | 递归进入 `shadowRoot` | 递归进入 `shadowRoot` + slot 分配 |

### 4.2 可见性过滤

| 对比维度 | Web (本项目) | Playwright |
|---|---|---|
| **过滤模式** | 单一 (ARIA 可见性) | 三种: `aria` / `ariaOrVisible` / `ariaAndVisible` |
| **隐藏判断** | `display:none` / `visibility:hidden` / `opacity:0` / `aria-hidden=true` / 祖先链传播 | 同左 + `Element.checkVisibility()` API + `display:contents` 特殊处理 |
| **特殊标签跳过** | `SCRIPT` / `STYLE` / `NOSCRIPT` / `TEMPLATE` | 同左 |
| **隐藏父节点的可见子节点** | 重新挂载到最近可见祖先 | 同左 |
| **视觉可见性** | 不检查 | `ariaOrVisible` 模式下额外检查 `getBoundingClientRect` 非零尺寸 |

### 4.3 Role 确定

| 对比维度 | Web (本项目) | Playwright |
|---|---|---|
| **优先级** | explicit `role` → implicit (by tag) → `generic` | explicit `role` → implicit (by tag) → 无角色则跳过 |
| **explicit role** | 直接取 `getAttribute("role")`，匹配快查表 | 同左 + 空格分隔取第一个有效 role |
| **implicit role** | HTML 标签 → role 映射表 (约 50 个标签) | 同左，完整 HTML-AAM 实现 |
| **presentation/none 冲突** | 不处理 (presentation 节点跳过，子节点提升) | 冲突解决: 若有全局 ARIA 属性或可聚焦，则忽略 presentation，使用 implicit role |
| **SVG** | `image` role，`<title>` 作为 name 来源 | `img` role，`<title>` 作为 name 来源 |
| **未知 role** | → `generic` | → 跳过该节点 |

### 4.4 Name 计算

| 对比维度 | Web (本项目) | Playwright |
|---|---|---|
| **算法参照** | 简化版 W3C accname | 完整 W3C accname (步骤 2a-2i) |
| **aria-labelledby** | 支持 (递归计算引用元素 name) | 支持 + 循环检测 (`visitedElements`) |
| **aria-label** | 支持 | 支持 |
| **label 元素关联** | `label[for]` + 包裹式 `<label>` | 同左 + `<label>` 内嵌控件时排除控件自身文本 |
| **HTML 原生命名** | `img[alt]` / `input[value]` / `input[placeholder]` / `textarea[placeholder]` / `select>option:selected` / `fieldset>legend` | 完整 HTML-AAM: 含 `button[value]` / `input[type=file]` 特殊处理 / `figure>figcaption` / `table>caption` / `svg>title` 等 |
| **nameFromContent** | 支持 (从子节点文本拼接 name) | 支持 + 嵌入式控件特殊处理 (textbox→value, combobox→选中项文本) |
| **::before / ::after** | `getComputedStyle().content` | 同左 |
| **title 回退** | 支持 | 支持 |
| **禁止命名** | 无限制 (所有节点都可命名) | `generic` / `paragraph` 等角色不允许从内容获取 name |

### 4.5 State 提取

| State | Web (本项目) | Playwright |
|---|---|---|
| **checked** | `aria-checked` + native `.checked` | 同左，仅限 checkbox/radio/switch/treeitem 等 role |
| **disabled** | `aria-disabled` + native `disabled` | 同左 + disabled fieldset/optgroup 祖先传播 |
| **expanded** | `aria-expanded` + `<details>.open` | 同左 |
| **pressed** | `aria-pressed` | 同左，仅限 `button` role |
| **selected** | `aria-selected` + native `<option>.selected` | 同左 |
| **level** | 不提取 | `aria-level` + `<h1>-<h6>` 原生映射 |
| **readonly** | `aria-readonly` + native `readOnly` | 不输出 (仅内部使用) |
| **invalid** | `aria-invalid` | 不输出 (仅内部使用) |
| **focused** | `document.activeElement` 匹配 | 不输出 |
| **value** | `aria-valuenow` / input.value / select.value | 不输出 (作为 prop 显示) |
| **cursor** | 不检测 | `getComputedStyle().cursor === 'pointer'` (AI 模式) |
| **Role gate** | 有 (每个 state 仅在特定 role 上提取) | 有 (每个 state 仅在特定 role 上提取) |

### 4.6 树优化

| 优化 | Web (本项目) | Playwright |
|---|---|---|
| **连续 text 合并** | 相邻 text 节点合并为一个 | `normalizeStringChildren()` 合并相邻文本 |
| **generic 折叠** | 无 name/state/ref 且仅一个子节点的 generic → 用子节点替换 | `normalizeGenericRoles()` 单 ref 子节点 generic → 折叠 |
| **重复子文本去除** | 子 text 内容 == 父 name → 去除子节点 | `textContributesInfo()` LCS 比较，无增量信息则去除 |
| **name 拼接优化** | 无 | 嵌入式控件 (textbox.value 等) 替代为文本内容 |

### 4.7 Ref 分配与渲染

| 对比维度 | Web (本项目) | Playwright |
|---|---|---|
| **Ref 格式** | `w1`, `w2`, ... | `e1`, `e2`, ... |
| **分配范围** | 所有有效 bounds 节点 | 可聚焦或可交互节点 |
| **bounds 格式** | `left,top,right,bottom` (绝对像素) | `x,y,width,height` (CSS 像素) |
| **YAML 格式** | `- role "name" [state] [ref=w1] [bounds=...]` | `- role "name" [state] [ref=e1]` |
| **Prop 输出** | 无 | `link` 节点输出 `/url: ...`; `textbox` 输出 `/placeholder: ...` |
| **增量 diff** | 不支持 | 支持 (传入 previousSnapshot，只渲染变化部分) |

---

## 5. 输出格式规范

### 5.1 节点语法

```text
- {role} "{name}" [state] [ref={ref}] [bounds={left},{top},{right},{bottom}]
```

### 5.2 字段说明

| 字段 | 必需 | 说明 |
|---|---:|---|
| `role` | 是 | 语义角色，见下方角色表 |
| `name` | 否 | 可读名称或文本内容 |
| `state` | 否 | `disabled` / `checked` / `selected` / `focused` / `scrollable` / `password` / `clickable` / `clickable=inferred` / `clickable=guessed` / `value=...` |
| `ref` | 否 | 可操作节点引用，native 前缀 `n`，web 前缀 `w` |
| `bounds` | 否 | 绝对像素坐标 `left,top,right,bottom` |
| 层级 | 否 | 缩进表示父子关系，父节点以 `:` 结尾 |

### 5.3 示例

```yaml
- screen:
  - toolbar "My App":
    - button "Search" [ref=n1]
    - button "Settings" [ref=n2]
  - list:
    - listitem [clickable] [ref=n3]:
      - text "Message 1"
      - text "Preview text"
    - listitem [clickable=guessed] [ref=n4]:
      - text "Message 2"
```

---

## 6. 角色表

### 6.1 Native + Web 共享

| Role | 说明 |
|---|---|
| `screen` | 根节点 |
| `toolbar` | 工具栏 / ActionBar / AppBar |
| `tabbar` | Tab 布局容器 |
| `tab` | Tab 项 |
| `list` | 列表容器 (RecyclerView / ListView) |
| `grid` | 网格容器 |
| `listitem` | 列表/网格子项 |
| `scroll` | 可滚动区域 |
| `card` | 卡片容器 |
| `section` | 语义分区 |
| `dialog` | 对话框 |
| `sheet` | BottomSheet |
| `text` | 纯文本 |
| `image` | 图片 / SVG |
| `button` | 可点击元素 |
| `input` | 输入框 |
| `checkbox` | 复选框 |
| `radio` | 单选按钮 |
| `switch` | 开关 |
| `slider` | 滑块 / SeekBar |
| `picker` | 选择器 (Spinner / DatePicker / TimePicker) |
| `progress` | 进度条 |
| `generic` | 通用容器 (无明确语义) |
| `webview` | WebView 容器 |
| `link` | 链接 |
| `heading` | 标题 |
| `navigation` | 导航区域 |

### 6.2 Web 专属 (HTML ARIA roles)

| Role | HTML 来源 |
|---|---|
| `textbox` | `<input type=text/email/...>` |
| `searchbox` | `<input type=search>` |
| `spinbutton` | `<input type=number>` |
| `combobox` | `<select>` / `<input role=combobox>` |
| `listbox` | `<select>` / `<datalist>` |
| `table` | `<table>` |
| `row` | `<tr>` |
| `cell` | `<td>` |
| `columnheader` | `<th scope=col>` |
| `rowheader` | `<th scope=row>` |
| `form` | `<form>` |
| `article` | `<article>` |
| `complementary` | `<aside>` |
| `blockquote` | `<blockquote>` |
| `caption` | `<caption>` |
| `group` | `<details>` / `<optgroup>` |
| `term` | `<dt>` |
| `definition` | `<dd>` |
| `separator` | `<hr>` |
| `meter` | `<meter>` |
| `option` | `<option>` |
| `status` | `<output>` |
| `paragraph` | `<p>` |
| `rowgroup` | `<thead>` / `<tbody>` / `<tfoot>` |
