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
- listitem [ref=n2]:
  - text "平台通知"
  - text "测试计划同步"
```

基本节点语法：

```text
- {role} "{name}" [ref={ref}]
```

支持层级：

```yaml
- listitem [ref=n10]:
  - text "梁晓舟"
  - text "明天上班"
  - text "04/22"
```

## 字段

| 字段 | 是否必需 | 说明 |
|---|---:|---|
| `role` | 是 | 节点语义类型，如 `text`、`button`、`listitem` |
| `name` | 否 | LLM 可读名称或文本内容 |
| `ref` | 否 | 可操作对象引用，用于后续定位或动作关联 |
| 层级 | 否 | 表示 UI 结构从属关系 |

当前评测主要使用 `role`、`name` 和 annotation 中的 `minCount` 做统一检查。

## 当前推荐 Role

```text
text
button
list
listitem
scroll
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
