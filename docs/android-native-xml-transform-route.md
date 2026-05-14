# Android 原生 XML 处理路线一总结

## 背景

路线一的目标是把 Android 原生页面抓取到的 raw view tree XML，转换成更适合模型消费和后续动作定位的 native semantic snapshot。该格式当前也是项目统一 transform 输出标准 [LLM Input Snapshot v1](llm-input-snapshot-standard.md) 的 native 实现。

原始 XML 保留了大量 View 层级、布局容器和系统节点。它适合完整还原页面结构，但直接给模型使用时存在几个问题：

- 数据量大，重复属性多。
- 语义弱，主要依赖 `class/text/resource-id/bounds/clickable/scrollable` 等底层字段。
- 可操作目标不集中，点击入口常被内部 TextView/ImageView 稀释。
- 部分集合控件 item 由父容器统一处理触摸，item 自身未必暴露 `clickable=true`。

## 数据清洗路线

### 总体流程图

```text
+------------------+
| Raw View XML     |
| dump / capture   |
+--------+---------+
         |
         v
+------------------+
| XML Parser       |
| NativeViewNode   |
+--------+---------+
         |
         v
+------------------+
| Role Resolver    |
| role / name      |
| state / bounds   |
+--------+---------+
         |
         v
+------------------+
| Tree Builder     |
| generic fold     |
| collection item  |
+--------+---------+
         |
         v
+------------------+
| Ref Assigner     |
| ref + bounds     |
+--------+---------+
         |
         v
+------------------+
| YAML Snapshot    |
| model input      |
+--------+---------+
         |
         v
+------------------+
| Action Executor  |
| ref -> bounds    |
| center tap       |
+------------------+
```

说明：本路线文档覆盖到 `YAML Snapshot` 产物；`Action Executor` 属于下游消费链路，用于说明 `ref + bounds` 如何被执行侧使用，不属于 transform 模块内部实现。

### 1. 原始 XML 解析

输入是 Android dump/capture 得到的 view tree XML。

解析阶段保留后续推理需要的核心字段：

- `class`
- `text`
- `content-desc`
- `resource-id`
- `bounds`
- `clickable`
- `long-clickable`
- `focusable`
- `enabled`
- `checked`
- `selected`
- `focused`
- `scrollable`
- `password`

这些字段被转换为内部 `NativeViewNode` 树，避免后续规则直接操作 XML 字符串。

### 2. Role 推导

基于 Android 场景定义 native role，而不是照搬 Web ARIA。

当前主要规则：

- `RecyclerView/ListView` -> `list`
- `GridView` -> `grid`
- `ScrollView/HorizontalScrollView` -> `scroll`
- `EditText` -> `input`
- `CheckBox/RadioButton/Switch/SeekBar/Spinner` 等 -> 对应表单 role
- `Button` 或通用节点带 `clickable/long-clickable` -> `button`
- `ImageView` -> `image`
- `TextView` -> `text`
- 其他布局容器 -> `generic`

补充：属性覆盖只在可交互 generic 类角色上生效（`text/image/generic` 等）。这类节点如果带 `clickable/long-clickable`，会被提升为 `button`（`attribute:clickable`）。

同时保留 `roleDecision.source`，例如：

- `class:list`
- `class:scroll`
- `attribute:clickable`
- `structure:collection-item`

这样后续可以调整规则并追踪 role 来源。

### 3. 状态与名称提取

节点名称优先级：

1. `text`
2. `content-desc`
3. 非文本节点的 `resource-id` 尾部可读名称

状态提取：

- `disabled`
- `checked`
- `selected`
- `focused`
- `scrollable`
- `password`

输出时以紧凑 YAML 风格表达：

```yaml
- button "搜索" [ref=n4] [bounds=42,312,870,407]
```

### 4. Generic 折叠

对无语义、无状态、无名称、只有单个子节点的 `generic` 包装层进行折叠。

目标是减少纯布局层级，但仍保留必要的分组结构，避免把多个兄弟语义节点错误合并。

### 5. Ref 分配

`ref` 是当前 snapshot 内的动作定位标识。

它不负责描述 UI 语义，而是让模型或后续执行器可以稳定引用某个具体可操作节点。

示例：

```yaml
- button "搜索" [ref=n4] [bounds=42,312,870,407]
```

模型可以输出：

```text
点击 n4
```

执行侧再通过 `ref=n4` 找回该节点，并用 `bounds` 计算点击点：

```text
x = (42 + 870) / 2
y = (312 + 407) / 2
```

最终注入点击事件。

`ref` 与其他字段的分工：

- `role`：节点是什么，例如 `button/listitem/input`。
- `name/text`：节点展示什么，例如“搜索”。
- `bounds`：节点在屏幕哪里。
- `ref`：动作执行时引用哪个节点。

相比让模型直接输出文字或坐标，`ref` 更适合动作执行：

- 页面里可能存在多个同名文本，例如多个“更多”“搜索”。
- 坐标直接暴露给模型不稳定，也不利于解释和回放。
- `ref` 短、明确，并且能和 snapshot 中的语义节点一一对应。

第一阶段只给明确可执行 role 分配 ref：

- `button`
- `input`
- `checkbox`
- `radio`
- `switch`
- `slider`
- `picker`
- `list`
- `grid`
- `scroll`
- `listitem`

要求节点必须有有效 `bounds`。

后续执行点击时可以用 `bounds` 中心点注入点击事件，不依赖 Java View 对象引用。

### 6. 集合控件 item 特殊处理

真机验证发现，常见集合控件存在一种情况：

- `RecyclerView.addOnItemTouchListener`
- `ListView.setOnItemClickListener`
- `GridView.setOnItemClickListener`

这些方式可以让运行时 item 可点，但 item root 在 raw XML 里不一定出现 `clickable=true`。

因此 transform 对集合控件做了分层处理：

- 父节点是 `list/grid` 时，直接结构子节点如果是 `generic/card/section`，标记为 `listitem`。
- `listitem` 会补充可点击状态：
  - `clickable`：item 自身可点击（`clickable=true` 或 `has-onclick-listener=true`）。
  - `clickable-inferred`：由容器信号推断可点击（`has-item-click-listener=true` 或 `has-item-touch-listener=true`）。
  - `clickable-guessed`：仅内部中间态，渲染输出时统一映射为 `clickable-inferred`，不对外暴露独立状态名。
- `listitem` 的 ref 分配按状态区分：
  - `clickable` / `clickable-inferred`：即使内部已有明确可执行子节点，父级 `listitem` 仍分配 ref。
  - `clickable-guessed`（内部态）：仅在不存在可执行后代节点时分配 ref；若已存在可执行后代则保持保守去重，优先保留子节点 ref。

示例：

```yaml
- list [ref=n7] [bounds=0,427,1080,2190]:
  - listitem [clickable-inferred] [ref=n8] [bounds=0,427,1080,677]:
    - text "平台通知"
    - text "测试计划同步"
```

对于搜索行这类内部已有明确按钮的集合子项，父子都会保留 ref：

```yaml
- listitem [clickable-inferred] [ref=n4]:
  - button "搜索" [ref=n5]
  - button [ref=n6]:
    - text "客服"
```
这样可以同时保留“整行点击”与“行内按钮点击”两种动作语义。

### 7. 输出格式

transform 输出为 `text/yaml`，文件扩展名为 `.yml`。

路径示例：

```text
captures/{baselineId}/runs/{runId}/native/raw/native_xml_{timestamp}.xml
captures/{baselineId}/runs/{runId}/native/transformed/native_semantic_snapshot_{timestamp}.yml
```

该 transformed 产物在评测侧统一视为 `native.llm_input`。跨插件统一格式见 [LLM Input Snapshot v1](llm-input-snapshot-standard.md)。

## 数据量对比

以下数据来自真机手工验收阶段的四个 native 首页样本。

| 页面 | runId | Raw XML bytes | Snapshot bytes | Snapshot / Raw | 降低比例 |
|---|---:|---:|---:|---:|---:|
| 消息 | 1777533579077 | 28,538 | 4,784 | 16.8% | 83.2% |
| 邮件 | 1777533581156 | 21,594 | 4,225 | 19.6% | 80.4% |
| 通讯录 | 1777533582718 | 22,162 | 4,000 | 18.0% | 82.0% |
| 业务 | 1777533586114 | 17,835 | 3,315 | 18.6% | 81.4% |
| 合计 | - | 90,129 | 16,324 | 18.1% | 81.9% |

观察结论：

- semantic snapshot 约为 raw XML 的 18%。
- 数据量平均降低约 82%。
- 降低数据量的同时保留了核心文本、role、状态、ref 和 bounds。
- 集合控件 item 在 `clickable=false` 的场景下也可以通过 `listitem [clickable-inferred] [ref=...]` 保留动作定位能力。

## 数据变化示例

下面是同一页面片段在 raw XML 和 semantic snapshot 中的形态对比。中间省略了大量重复属性和布局层级。

### 原始 XML 节选

```xml
<hierarchy activity="com.hh.uiperception.baseline.nativepage.NativeHomeActivity">
  <node class="com.android.internal.policy.DecorView" bounds="[0,0][1080,2400]" clickable="false">
    <node class="android.widget.FrameLayout" resource-id="android:id/content" bounds="[0,132][1080,2337]">
      ...
      <node class="android.widget.TextView" text="搜索"
            bounds="[42,312][870,407]"
            clickable="true" focusable="true" />
      <node class="android.widget.LinearLayout"
            bounds="[870,305][1038,415]"
            clickable="true" focusable="true">
        <node class="android.widget.ImageView" bounds="[941,309][999,367]" clickable="false" />
        <node class="android.widget.TextView" text="客服" bounds="[902,367][1038,410]" clickable="false" />
      </node>
      <node class="android.widget.ScrollView"
            bounds="[0,636][1080,2190]"
            scrollable="true" focusable="true">
        <node class="android.widget.LinearLayout" bounds="[0,636][1080,2337]">
          <node class="android.widget.LinearLayout"
                bounds="[0,825][1080,1014]"
                clickable="true" focusable="true">
            <node class="android.widget.TextView" text="辉" bounds="[42,851][147,956]" />
            <node class="android.widget.TextView" text="梁晓舟" bounds="[184,851][870,914]" />
            <node class="android.widget.TextView" text="明天上班" bounds="[184,925][1038,978]" />
          </node>
          ...
        </node>
      </node>
    </node>
  </node>
</hierarchy>
```

### Semantic Snapshot 节选

```yaml
- screen:
  - generic "content":
    - generic:
      - button "搜索" [ref=n4] [bounds=42,312,870,407]
      - button [ref=n5] [bounds=870,305,1038,415]:
        - image
        - text "客服"
      - scroll [scrollable] [ref=n14] [bounds=0,636,1080,2190]:
        - generic:
          - button [ref=n16] [bounds=0,825,1080,1014]:
            - text "辉"
            - generic:
              - text "梁晓舟"
              - text "明天上班"
```

## 当前能力边界

已完成：

- XML -> `NativeViewNode` 结构化解析。
- Android native role 推导。
- 状态、名称、bounds 提取。
- generic wrapper 折叠。
- ref 分配。
- `RecyclerView/ListView/GridView` item 特殊处理。
- YAML snapshot 输出。
- 真机验证消息、邮件、通讯录、业务页面。

仍待优化：

- 系统栏节点当前仍会保留为普通 `generic`，后续可以过滤。
- `button` 与 `listitem` 的语义边界还可以继续细化。
- `HorizontalScrollView` 中的 chip/tab 类入口还没有独立 role。
- 数据效果还需要进入路线二，通过静态指标和任务评测验证。
