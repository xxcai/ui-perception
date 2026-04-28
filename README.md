# UI Perception

UI Perception 是一个 Android 端 UI 感知研究项目，目标是对比不同 UI 抓取方向的效果，并把手机屏幕上的 UI 信息转换成适合大模型理解的数据表示。

本项目只关注 UI 感知数据的准备、采集、裁剪、融合和评测，不包含 Agent 决策、规划或执行操作。

## 项目边界

本项目关注：

- 基准页面与基准意图准备
- 当前页面内容采集
- 不同抓取方向的效果对比
- 多来源 UI 数据裁剪与清洗
- 面向大模型的数据表达
- 感知结果与基准意图的评测

本项目不关注：

- Agent 操作执行
- 自动点击、滑动、输入
- 任务规划与工具调用
- 多轮交互决策

## 模块结构

当前工程按“编排核心 + 技术方向插件 + 基准页面”组织。

```text
:app
├── depends on :baseline-pages
├── depends on :perception-core
└── depends on :native-plugin

:native-plugin
└── depends on :perception-core

:web-plugin
└── depends on :perception-core

:ocr-plugin
└── depends on :perception-core

:small-model-plugin
└── depends on :perception-core

:baseline-pages
└── no project-module dependency

:perception-core
└── no project-module dependency
```

模块职责：

- `:app`：应用入口、基准页面列表、浮动感知按钮、当前插件装配点。
- `:baseline-pages`：基准页面、页面路由、基准意图资产。
- `:perception-core`：感知编排核心，定义统一插件接口、请求/结果模型和 `PerceptionComposer`。
- `:native-plugin`：Native 技术方向工具集，已实现 native 页面抓取与 native XML 裁剪。
- `:web-plugin`：Web 技术方向占位模块，尚未实现插件类。
- `:ocr-plugin`：OCR 技术方向占位模块，尚未实现插件类。
- `:small-model-plugin`：小模型视觉理解方向占位模块，尚未实现插件类。

## 当前链路

当前 `:app` 只装配了 `NativePerceptionPlugin`，所以一次点击只执行 native 方向。

```text
基准页面
  ↓
浮动感知按钮
  ↓
PerceptionPlan
  └── plugins = [NativePerceptionPlugin]
  ↓
PerceptionComposer
  ↓
NativePerceptionPlugin.capture()
  ↓
raw native XML
  ↓
NativePerceptionPlugin.trim()
  ↓
trimmed native XML
  ↓
captures/{baselineId}/runs/{runId}/native/raw
captures/{baselineId}/runs/{runId}/native/trimmed
```

已验证的 native 产物示例：

```text
captures/native_home_mail/runs/{runId}/native/raw/native_xml_{timestamp}.xml
captures/native_home_mail/runs/{runId}/native/trimmed/native_xml_basic_trim_{timestamp}.xml
```

## 核心接口

技术方向插件统一实现 `PerceptionPlugin`。

```java
public interface PerceptionPlugin {
    String name();

    CaptureResult capture(Activity activity, CaptureRequest request);

    TrimResult trim(TrimRequest request);
}
```

约定：

- 一个插件代表一个技术方向，例如 `native`、`web`、`ocr`、`small_model`。
- 插件本身不保存页面状态，作为无状态工具集使用。
- `capture()` 负责产出本技术方向的原始数据。
- `trim()` 负责裁剪或清洗该技术方向的原始数据。
- 暂不支持的阶段直接返回 `null`，由 `PerceptionComposer` 转成错误结果。
- 具体实现可以拆成包内 helper，但对外只暴露一个插件类。

当前 native 插件形态：

```text
native-plugin
├── NativePerceptionPlugin
│   ├── capture() -> NativeViewCaptureTool
│   └── trim()    -> NativeXmlTrimTool
├── ViewHierarchyDumper
└── XmlTrimmer
```

## Composer

`PerceptionComposer` 是多抓取方向对比的编排层。一个 `PerceptionPlan` 可以包含多个 `PerceptionPlugin`，composer 会在同一个 baseline 页面上依次执行每个插件。

```text
PerceptionPlan
├── baselineId
├── runId
├── trimEnabled
└── plugins
    ├── NativePerceptionPlugin
    ├── WebPerceptionPlugin
    ├── OcrPerceptionPlugin
    └── SmallModelPerceptionPlugin
```

执行方式：

```text
plugin A: capture -> optional trim
plugin B: capture -> optional trim
plugin C: capture -> optional trim
```

输出结构：

```text
PerceptionRunResult
├── baselineId
├── runId
├── startedAtMs
├── finishedAtMs
└── entries
    ├── PerceptionEntryResult(native)
    ├── PerceptionEntryResult(web)
    ├── PerceptionEntryResult(ocr)
    └── PerceptionEntryResult(small_model)
```

当前实现是顺序执行，不做并发、不做融合、不做评测。

## 数据产物

当前抓取产物写入 app 外部文件目录，并按一次 run 分组：

```text
captures/{baselineId}/runs/{runId}/{pluginName}/raw/{source}_{timestamp}.{ext}
captures/{baselineId}/runs/{runId}/{pluginName}/trimmed/{source}_{timestamp}.{ext}
```

这样同一次页面抓取中，native、web、ocr、小模型的结果可以放在同一个 `runId` 下，方便后续对比。

当前 native 链路输出：

- raw：完整原生 View 层级 XML。
- trimmed：只保留 `index`、`class`、`resource-id`、`text`、`bounds` 的 XML。

## 添加 Web 能力

`web-plugin` 模块已经存在，只需要补实现，不需要再创建模块。

建议文件形态：

```text
web-plugin
├── WebPerceptionPlugin
├── WebDomCaptureTool
└── WebDomTrimTool
```

实现 `WebPerceptionPlugin`：

```java
public final class WebPerceptionPlugin implements PerceptionPlugin {
    @Override
    public String name() {
        return "web";
    }

    @Override
    public CaptureResult capture(Activity activity, CaptureRequest request) {
        // 从 WebView 或页面上下文获取 DOM 原始数据。
        // 暂未实现时返回 null。
        return null;
    }

    @Override
    public TrimResult trim(TrimRequest request) {
        // 对 DOM 做裁剪、去噪、结构压缩。
        // 暂未实现时返回 null。
        return null;
    }
}
```

接入步骤：

- 在 `:app` 中装配 `WebPerceptionPlugin`。
- 把它加入 `PerceptionPlan` 的插件列表。
- 补充真实 Web 基准页。当前 `:baseline-pages` 只有 Web 占位入口，真实 Web DOM 验收前需要提供可稳定复现的 WebView 页面和基准意图资产。

## 添加 OCR 能力

`ocr-plugin` 模块已经存在，只需要补实现，不需要再创建模块。

建议文件形态：

```text
ocr-plugin
├── OcrPerceptionPlugin
├── ScreenshotCaptureTool
├── OcrEngineAdapter
└── OcrTrimTool
```

实现 `OcrPerceptionPlugin`：

```java
public final class OcrPerceptionPlugin implements PerceptionPlugin {
    @Override
    public String name() {
        return "ocr";
    }

    @Override
    public CaptureResult capture(Activity activity, CaptureRequest request) {
        // 获取截图并执行 OCR，输出原始 OCR 结果。
        // 暂未实现时返回 null。
        return null;
    }

    @Override
    public TrimResult trim(TrimRequest request) {
        // 合并文本块、过滤噪声、规范化坐标和置信度。
        // 暂未实现时返回 null。
        return null;
    }
}
```

OCR 输出建议：

- raw 输出保留 OCR 引擎原始信息。
- trimmed 输出稳定 JSON，至少包含文本、bounds、置信度和阅读顺序。

## 添加小模型能力

`small-model-plugin` 模块已经存在，只需要补实现，不需要再创建模块。

建议文件形态：

```text
small-model-plugin
├── SmallModelPerceptionPlugin
├── SmallModelCaptureTool
└── SmallModelTrimTool
```

建议插件名：

```java
public final class SmallModelPerceptionPlugin implements PerceptionPlugin
```

职责建议：

- `capture()`：输入截图或页面上下文，产出小模型原始理解结果。
- `trim()`：压缩小模型输出，保留对象、文本、区域、层级或动作相关语义。

## 开发原则

- 新技术方向优先实现一个 `PerceptionPlugin`，不要先改 composer。
- 插件对外保持单一入口，内部可以拆无状态 helper。
- capture 输出原始信息，trim 输出适合模型消费的裁剪结果。
- 不要在 capture 内部私自做 trim。
- 不要在插件内部做跨技术方向融合；融合应由后续 composer 或独立编排层负责。
- 每个新增能力都应提供可验证的基准页面、输入、输出和验收方式。

## 当前状态

已完成：

- Native 基准页面：消息、邮件、通讯录、业务。
- Native 原始 XML 抓取。
- Native XML 裁剪。
- `:perception-core` 多插件 plan / run / entry 结果结构。
- 通过浮动按钮触发 native capture -> trim 链路。
- `:web-plugin`、`:ocr-plugin`、`:small-model-plugin` 模块占位。

未完成：

- 真实 Web 基准页与 Web DOM 抓取。
- OCR 截图、识别与裁剪能力。
- 小模型视觉理解能力。
- 多源融合。
- 自动评测链路。
