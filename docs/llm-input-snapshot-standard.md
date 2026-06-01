# LLM Input Snapshot v1

`LLM Input Snapshot v1` 是本项目当前的统一 transform 输出标准。

它用于承载各采集方式实际提供给 LLM 的页面信息，也是端上评测直接读取的对象。Native、Web、OCR、小模型等插件可以有不同的采集方式，但 transformed 输出必须收敛到这个标准。

## 产物位置

参与评测的 transformed 产物应写入：

```text
captures/{baselineId}/runs/{runId}/{pluginName}/transformed/llm_input_{timestamp}.yml
```

当前 native 旧文件名继续兼容：

```text
captures/{baselineId}/runs/{runId}/native/transformed/native_semantic_snapshot_{timestamp}.yml
```

评测结果中统一识别为：

```json
{
  "id": "{plugin}.llm_input",
  "plugin": "{plugin}",
  "stage": "transformed",
  "type": "llm_input"
}
```

## 格式

`LLM Input Snapshot v1` 沿用当前 native semantic snapshot 的 YAML 风格文本结构：

```yaml
- text "消息"
- button "搜索" [ref=n1]
- listitem [clickable-inferred] [ref=n2]:
  - text "平台通知"
  - text "测试计划同步"
```

基本节点语法：

```text
- {role} "{name}" [state] [ref={ref}] [bounds={left},{top},{right},{bottom}]
```

支持层级：

```yaml
- listitem [clickable-inferred] [ref=n10]:
  - text "梁晓舟"
  - text "明天上班"
  - text "04/22"
```

## 字段

| 字段 | 是否必需 | 说明 |
|---|---:|---|
| `role` | 是 | 节点语义类型，如 `text`、`button`、`listitem` |
| `name` | 否 | LLM 可读名称或文本内容 |
| `state` | 否 | 节点状态信息，如 `scrollable`、`disabled`、`clickable`、`clickable-inferred` |
| `ref` | 否 | 可操作对象引用，用于后续定位或动作关联 |
| `bounds` | 否 | 元素在屏幕中的位置，格式为 `left,top,right,bottom`，用于动作定位、增强结果回填和问题复查 |
| 层级 | 否 | 表示 UI 结构从属关系 |

说明：

- YAML 中 role 后的引号文本统一称为 `name`。
- `text` 是一种 role，不是独立字段；`- text "消息"` 表示 `role=text`、`name=消息`。
- `bounds` 不直接表达语义，但对 `ref` 定位、OCR / 小模型结果对齐很重要。能稳定获得位置的插件应尽量保留。

关于列表项可点击状态：

- `clickable`：节点自身具备明确点击信号。
- `clickable-inferred`：由列表容器信号推断可点击（例如 `ListView` item click listener 或 `RecyclerView` item touch listener）。

当前评测主要使用 `role`、`name` 和 annotation 中的 `minCount` 做统一检查。

## 当前推荐 Role

```text
text
button
input
checkbox
radio
switch
slider
picker
list
listitem
scroll
grid
image
visual_state
```

新增 role 时应满足：

- 能被多个插件通过统一格式表达。
- 语义边界清楚，不能只服务某一个插件的内部实现。
- annotation 与评测逻辑能用同一套规则检查。

## 插件职责

各插件负责把自己的原始采集结果转换为 `LLM Input Snapshot v1`：

```text
native-plugin:
  raw XML -> LLM Input Snapshot

web-plugin:
  DOM / A11y / HTML -> LLM Input Snapshot

ocr-plugin:
  Screenshot / OCR blocks -> LLM Input Snapshot

small-model-plugin:
  模型理解结果 -> LLM Input Snapshot
```

## 增强回填约定

Native、OCR、小模型可以按“基础结构 + 字段增强”的方式协作：

```text
Native 基础 YAML -> OCR 文本补盲 -> 小模型语义补充 -> 增强版 YAML
```

推荐分工：

| 来源 | 主要贡献 | 回填方式 |
|---|---|---|
| Native | 结构、位置、基础交互、已有文本 | 生成基础 `role`、`name`、`state`、`ref`、`bounds` 和层级 |
| OCR | Native 无法稳定获取的可见文字 | 补充或修正对应区域的 `name` / `text` 节点 |
| 小模型 | 无文本图标、按钮意图、区域功能 | 补充元素 `name`，必要时建议更合适的 `role` |

增强结果应优先回填到已有节点，而不是另起一套平行结构。回填时至少需要保留来源信息在调试产物中，便于复查冲突来源；正式 `LLM Input Snapshot v1` 可以先保持简洁 YAML 表达。

冲突处理原则：

- Native 已明确给出结构和可操作性时，OCR / 小模型不应覆盖 `ref` 和 `bounds`。
- OCR 更适合补文字，小模型更适合补视觉语义；二者同时命中时，应按场景区分 `name` 的来源。
- 如果增强结果置信度不足，宁可保留无名可操作元素，也不要强行写入误导性 `name`。

## 原则

- transform 输出就是实际给 LLM 的输入。
- 不额外生成专门用于测评的中间格式。
- 所有插件的 transformed 输出格式一致。
- `:evaluation` 只评测 `llm_input`。
- annotation 和检查规则对所有插件一致。

## 最小合格示例

OCR 如果只能识别文本，第一版可以输出：

```yaml
- text "消息"
- text "搜索"
- text "客服"
- text "梁晓舟"
- text "04/22"
```

它不应该为了通过测评强行输出：

```yaml
- button "搜索"
```

除非 OCR 侧确实能稳定判断这是可操作入口。

## 评测匹配方式

annotation 示例：

```yaml
- id: search-entry
  role: button
  name: 搜索
  minCount: 1
```

检查逻辑：

```text
在 llm_input 中统计：
role == button
name == 搜索
数量 >= 1
```

这个规则对 Native、Web、OCR、小模型等所有插件一致。
