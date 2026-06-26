# 鸿蒙 ArkUI「可点击」语义探索

> 问题：`getFilteredInspectorTree()` 拿不到 `clickable` 字段，ArkUI 里「点击能力」如何表达？
> 状态：✅ 已查清，给出可落地的三档推断策略

## 1. 起因

V5 算法移植阶段，需要把鸿蒙 inspector 输出对接到 `SemanticNode`（含 role / name / bounds / clickable / enabled / checked / selected 等字段）。鸿蒙端：
- `$type` → role（类型映射）
- `attrs.content` → name（文本内容）
- `$rect` → bounds
- `attrs.enabled` → enabled
- `attrs.selected` → selected
- **`clickable` 字段缺失** —— 探索补全方案

## 2. 探索过程

### 2.1 直接查 SDK

```bash
grep -rnE "clickable\s*\(" $SDK/component $SDK/api
```

**结果**：`clickable` 在 ArkUI 公共 API 中**不存在**，只在测试框架 `@ohos.UiTest.d.ts` 里作为查询谓词出现：

```typescript
clickable(b?: boolean): By;   // 用于查找可点击组件
```

**结论**：ArkUI 没有「clickable」这个 stored attribute。`uitest dumpLayout` 之所以能输出 clickable=true，是因为测试框架有内部 onClick 注册状态的访问权，公开 API 拿不到。

### 2.2 实测 B 输出（全量样本）

对 Button / Toggle / ListItem / Text 四种节点，扫所有 attr 找 click 相关字段：

| 节点 | touchable | focusable | role | 类型特有字段 |
|---|---|---|---|---|
| Button | `'true'` | `'true'` ✅ | `role: 'ButtonRole.NORMAL'` ✅ | - |
| Toggle | `'true'` | `'true'` ✅ | (无) | `selectedColor` / `unselectedColor` |
| ListItem | `'true'` | `'false'` ⚠️ | (无) | `selectable` / `selected` / `swipeAction` |
| Text | `'true'` ⚠️ | `'false'` | (无) | - |

**关键观察**：
- `touchable` 全是 `'true'` —— 仅表示「能接收触摸事件」（hit test 通过），**不代表有 onClick**
- `focusable` 与 clickability **部分相关**：Button/Toggle 是 true，Text 是 false，但 ListItem 反例（clickable 但 focusable=false）
- `role` 字段只出现在 Button 上（`ButtonRole.NORMAL`），其他组件没有
- 状态字段（`selectable` / `selected` / `selectedColor`）按类型分散，不是统一信号

### 2.3 显式 a11y API（开发者主动标记）

SDK 里找到三个相关 API：

```typescript
accessibilityRole(role: AccessibilityRoleType): T          // @since 18
accessibilityActionOptions(option: AccessibilityActionOptions): T   // @since 23
accessibilityScrollTriggerable(isTriggerable: boolean): T  // 显式声明可滚动
```

`AccessibilityAction` 枚举含 `ACCESSIBILITY_CLICK = 1`，开发者可以显式声明组件支持 click。

**问题**：这些 API 都是 opt-in，默认 ArkUI 组件不写就不出现。覆盖率低。

## 3. 结论：clickable 在 ArkUI 里是「类型 + 推断」而非「属性」

ArkUI 声明式模型下，**点击能力更多由组件类型决定**，而不是事件状态：
- `Button` / `Toggle` / `Checkbox` → 天生可点击，开发者不用做任何事
- `Text` / `Image` → 默认不可点击，但可以 `.onClick(...)` 注册后变可点击
- `ListItem` / `GridItem` → 容器型，由父 List/Grid 控制是否响应

**这跟 Android View / iOS UIView 模型根本不同**：
- Android：`clickable` 是显式 XML 属性 / `setOnClickListener` 后自动设 true
- iOS：`UIControl` 子类自带 touch tracking，普通 UIView 通过 gestureRecognizer 添加
- 鸿蒙：**类型即行为**，事件注册是「执行时机」而非「能力声明」

## 4. 三档推断策略（鸿蒙 RoleResolver 实现）

| 档位 | 信号来源 | 适用场景 | 准确度 |
|---|---|---|---|
| **L1: 按类型默认表** | `$type` ∈ INHERENTLY_CLICKABLE | Button/Toggle/Checkbox/... 等天生可点击的组件 | 🟢 高 |
| **L2: `focusable: 'true'` 推断** | `attrs.focusable === 'true'` && L1 未命中 | 非默认类型但被开发者注册 onClick 时（如 `Text.focusable(true).onClick(...)`） | 🟡 中（ListItem 反例） |
| **L3: 显式 a11y 标记** | `attrs.accessibilityRole` / `accessibilityActionOptions` 不空 | 开发者主动标记（少数） | 🟢 高（覆盖率低） |

### 4.1 鸿蒙 type → clickable 默认表（初版）

基于 SDK grep + 实测：

```typescript
const INHERENTLY_CLICKABLE: Set<string> = new Set([
  // 容器型 - 由父组件控制
  'ListItem', 'GridItem', 'MenuItem',
  // 选项型 - 显式交互组件
  'Button', 'Toggle', 'Checkbox', 'CheckboxGroup',
  'Radio', 'RadioButton', 'Stepper', 'Select',
  // 滑动型
  'Slider', 'Rating', 'Gauge',
  // 容器切换
  'Swiper', 'Tabs', 'TabContent',
  // 选择器
  'DatePicker', 'TimePicker', 'TextPicker', 'CalendarPicker',
]);

const INHERITLY_CHECKABLE: Set<string> = new Set([
  'Toggle', 'Checkbox', 'Radio', 'RadioButton',  // 二态
]);

const INHERITLY_SELECTABLE: Set<string> = new Set([
  'ListItem', 'GridItem', 'MenuItem',  // 列表项选中态
]);
```

### 4.2 跨端对比

| 平台 | clickable 主信号 | 次信号 |
|---|---|---|
| Android | `view.isClickable` (XML 属性) | `setOnClickListener` 后自动 true |
| iOS | `view is UIControl` 类型判断 | `gestureRecognizers != nil` |
| **鸿蒙** | `$type` 类型判断 | `attrs.focusable === 'true'` |

**结论**：跨端 capture 算法都需要「类型推断」+「状态字段」组合，鸿蒙并不算退步，反而是声明式模型的「正确表达」。

## 5. 对 SDK 实现的影响

### 5.1 SemanticNode 字段映射（更新版）

| SemanticNode 字段 | 鸿蒙来源 | 备注 |
|---|---|---|
| role | `$type` 映射表 | 需要独立的 `HarmonyRoleResolver` |
| name | `attrs.content` ‖ `attrs.label` ‖ `attrs.accessibilityText` | 优先级链 |
| bounds | `$rect` 解析 | 格式 `[l,t][r,b]` 与 Android 一致 |
| ref | `$ID` 数字 | 与 `n1/n2` 格式转换 |
| enabled | `attrs.enabled === 'true'` | |
| clickable | **三档推断**（L1 type / L2 focusable / L3 a11y） | 见本文 §4 |
| checked | `attrs.selected === 'true'`（仅 INHERITLY_CHECKABLE 类型） | |
| selected | `attrs.selected === 'true'` | |
| scrollable | `attrs.scrollable === 'true'` ‖ `accessibilityScrollTriggerable === 'true'` | |

### 5.2 推荐的 filter 配置

实测三种 filter 配置（`capture_app_B_filter{0,1,2}.json`）：

```typescript
const SEMANTIC_FILTERS = [
  'content',
  'enabled',
  'focusable',           // L2 clickable 推断信号
  'selected',            // checked/selected 状态
  'selectable',          // ListItem 选中能力
  'scrollable',
  'accessibilityText',
  'accessibilityDescription',
  'accessibilityRole',   // L3 显式标记
  'accessibilityActionOptions',
];
```

输出约 13KB / 33 节点 / 每节点 ~15 字段。比无 filter 175KB 节省 92%，且含所有 clickable 推断所需信号。

## 6. 已采集样本

- `docs/harmonyos-samples/capture_app_A_framenode.json` —— FrameNode 递归输出（9880b，结构稀疏）
- `docs/harmonyos-samples/capture_app_B_inspector.json` —— InspectorTree 无 filter（175650b，全量噪声）
- `docs/harmonyos-samples/capture_app_B_filter0_content.json` —— filter=[content]（9443b）
- `docs/harmonyos-samples/capture_app_B_filter1_semantic.json` —— filter=[content,enabled,label,accessibilityText,accessibilityDescription]（12862b）
- `docs/harmonyos-samples/capture_app_B_filter2_state.json` —— filter=[content,enabled,clickable,checked,selected,scrollable,accessibilityText]（12494b）

## 7. 下一步

V5（跨端算法移植）的输入已就绪：
- ✅ 知道抓什么（getFilteredInspectorTree + SEMANTIC_FILTERS）
- ✅ 知道字段映射（type → role / content → name / focusable → clickable L2）
- ✅ 知道鸿蒙特有约束（声明式模型，无显式 clickable）

可以启动 ArkTS 版 `Bounds` / `HarmonyRoleResolver` / `RefAssigner` / `SnapshotRenderer` 移植。
