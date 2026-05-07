# 其他采集方式评测接入说明

本文说明 Web、OCR、小模型等采集方式如何接入当前端上评测框架。核心原则是：被测评对象必须是实际提供给 LLM 的输入，不额外生成只用于测评的中间产物。

统一 transform 输出格式见 [LLM Input Snapshot v1](llm-input-snapshot-standard.md)。

## 核心原则

- 各插件可以有不同的采集方式。
- 各插件的 transformed 产物必须收敛到同一种 LLM 输入格式。
- 评测只读取 transformed LLM 输入。
- 同一页面使用同一份 annotation。
- 同一套检查规则适用于所有插件。
- 当前阶段不做多源融合，只评估每种采集方式单独提供了多少有效数据。

```text
Native / Web / OCR / 小模型
        |
        v
各自 capture 原始数据
        |
        v
各自 transform 到标准 LLM 输入
        |
        v
:evaluation 读取同一格式
        |
        v
同一份 annotation + 同一套规则
        |
        v
evaluation-result.json
```

## 目录约定

每次抓取按同一个 runId 落盘：

```text
captures/{baselineId}/runs/{runId}/{pluginName}/raw/
captures/{baselineId}/runs/{runId}/{pluginName}/transformed/
```

如果某个插件要参与评测，需要输出：

```text
{pluginName}/transformed/llm_input_{timestamp}.yml
```

示例：

```text
ocr/transformed/llm_input_1778135013775.yml
web/transformed/llm_input_1778135013775.yml
small-model/transformed/llm_input_1778135013775.yml
```

当前 native 旧文件名继续兼容：

```text
native/transformed/native_semantic_snapshot_{timestamp}.yml
```

评测结果中会统一识别为：

```json
{
  "id": "native.llm_input",
  "plugin": "native",
  "stage": "transformed",
  "type": "llm_input"
}
```

## LLM 输入格式

标准 LLM 输入格式沿用当前 native semantic snapshot 的文本结构，完整标准见 [LLM Input Snapshot v1](llm-input-snapshot-standard.md)。

```yaml
- text "消息"
- button "搜索" [ref=n1]
- listitem [ref=n2]:
  - text "平台通知"
```

当前评测优先使用 role、name 和 minCount 做统一检查。其他插件不需要实现 native 内部逻辑，只需要把自己的 transformed 输出写成同一套 LLM 输入格式。

## Annotation 约定

annotation 描述页面应该提供哪些有效信息，而不是描述某个插件的产物格式。

示例：

```yaml
targets:
  - id: search-entry
    type: information
    description: LLM 能否知道页面提供搜索入口
    evidence:
      - id: search-button
        capability: actionable
        role: button
        name: 搜索
        minCount: 1
```

这条 evidence 对所有插件使用同一把尺子：

```text
在该插件的 llm_input 中，统计 role=button 且 name=搜索 的节点数量是否 >= 1
```

如果 OCR 第一版只能确认“搜索”文字可读，不能确认它是按钮，就应该只输出：

```yaml
- text "搜索"
```

此时 `role: text, name: 搜索` 的 evidence 可以通过，`role: button, name: 搜索` 的 evidence 不应通过。这能体现 OCR 采集方式的真实能力边界。

## OCR Plugin 接入

OCR 侧需要完成：

```text
截图或 OCR 原始结果
        |
        v
文本块识别、去噪、排序、合并
        |
        v
ocr/transformed/llm_input_{timestamp}.yml
```

第一版建议只输出有把握的文字节点：

```yaml
- text "消息"
- text "搜索"
- text "客服"
- text "梁晓舟"
- text "04/22"
```

不要为了通过测评强行推断 `button`、`listitem`、`visual_state`。如果 OCR 采集方式无法稳定提供这些信息，让对应 evidence 失败是合理结果。

## Web Plugin 接入

Web 侧需要完成：

```text
DOM / Accessibility / HTML 原始数据
        |
        v
结构清洗、可读文本提取、可操作元素识别
        |
        v
web/transformed/llm_input_{timestamp}.yml
```

示例：

```yaml
- text "邮件"
- button "搜索" [ref=w1]
- listitem [ref=w2]:
  - text "平台通知"
  - text "测试计划同步"
```

Web 如果能从 DOM 或 accessibility 信息中稳定识别按钮、列表、输入框，应输出对应 role。

## 小模型 Plugin 接入

小模型侧需要完成：

```text
截图 / 页面上下文 / 模型原始输出
        |
        v
把模型理解结果整理成标准 LLM 输入
        |
        v
small-model/transformed/llm_input_{timestamp}.yml
```

示例：

```yaml
- text "业务"
- text "最近使用"
- text "华为Wi-Fi"
- image "服务卡片预览"
```

小模型输出同样不应使用专属评分规则。能稳定表达为标准 role/name 的信息才进入 `llm_input`。

## 评测结果

符合契约的 transformed 产物会进入 `candidates`：

```json
{
  "id": "ocr.llm_input",
  "plugin": "ocr",
  "stage": "transformed",
  "type": "llm_input",
  "path": "ocr/transformed/llm_input_1778135013775.yml",
  "contentType": "text/yaml",
  "schemaStatus": "PASS"
}
```

随后 `:evaluation` 会读取该文件，并用同一份 annotation 生成：

- `targetResults`：页面信息意图是否满足。
- `evidenceResults`：每条证据是否命中。
- `summary`：当前页面整体表现。

## 接入验收

新插件接入后，至少验证以下内容：

1. `raw/` 下保留原始采集产物。
2. `transformed/` 下生成 `llm_input_*.yml`。
3. `evaluation-result.json` 的 `candidates` 中出现 `{plugin}.llm_input`。
4. `targetResults` 和 `evidenceResults` 正常生成。
5. 手机端结果页能展示该次评测结果。

当前可用命令：

```bash
./gradlew :evaluation:testDebugUnitTest --tests com.hh.uiperception.evaluation.OnDeviceEvaluationRunnerTest
./gradlew :app:compileDebugJavaWithJavac
./gradlew app:installDebug
```

手机端验证路径：

```text
打开基准页面
  -> 点击悬浮抓取按钮
  -> 自动跳转评测结果页
  -> 检查 candidates / 信息意图 / evidence 明细
```
