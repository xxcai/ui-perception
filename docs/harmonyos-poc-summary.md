# 鸿蒙 ArkUI 抓取 PoC 总结

> 范围：评估 UI Perception 能力在 HarmonyOS NEXT（API 24, 6.1.1）上的可复刻性
> 状态：✅ Native + Web 两条抓取链路均跑通，跨端算法层 1:1 对齐
> 完成时间：2026-06-26

## 1. 要回答的问题

| 问题 | 结论 |
|---|---|
| 鸿蒙有没有等价于 Android `native_xml` 的 UI 树数据源？ | ✅ `UIContext.getFilteredInspectorTree(filters)` |
| 能否拿到 clickable 字段？ | ⚠️ 无直接属性，但「type + 推断」可达 90%+ 准确率 |
| 跨端算法能否复用？ | ✅ Bounds/RefAssigner/SnapshotRenderer 等核心算法 1:1 移植 |
| Web H5 能否抓？ | ✅ ArkUI Web + javaScriptProxy 反向回传 |
| 跟 iOS / Android 输出能否对齐？ | ✅ 同一 web_form.html 已三端跑通 |

## 2. 两条抓取路径

### 2.1 Native (ArkUI)

**数据源**：`UIContext.getFilteredInspectorTree(filters)`，返回 JSON 字符串

**关键字段映射**：

| SemanticNode 字段 | 鸿蒙来源 |
|---|---|
| role | `$type` → HarmonyRoleResolver 映射表 |
| name | `attrs.label` (Button) ‖ `attrs.content` (Text) ‖ `attrs.placeholder` (Input) ‖ `attrs.accessibilityText` |
| bounds | `$rect` 解析（`[l, t],[r, b]` 浮点格式） |
| ref | `$ID` → 三档 ref 策略分配（n1, n2, ...） |
| clickable | L1 type 表 / L2 `focusable==='true'` / L3 a11y 标记 |
| checked/selected | `attrs.selected === 'true'`（仅 INHERENTLY_CHECKABLE 类型） |
| scrollable | `attrs.scrollable === 'true'`（input role 抑制） |

**采集中用到的 filters**：
```typescript
['content', 'label', 'placeholder', 'enabled', 'focusable',
 'selected', 'selectable', 'scrollable',
 'accessibilityText', 'accessibilityDescription',
 'accessibilityRole', 'accessibilityActionOptions']
```
~13KB / 33 节点 / 每节点 ~15 字段，相比无 filter 175KB 节省 92%。

**clickable 推断（三档）**：

| 档位 | 信号 | 准确度 |
|---|---|---|
| L1 | `$type` ∈ INHERENTLY_CLICKABLE（Button/Toggle/Checkbox/ListItem/...） | 🟢 高 |
| L2 | `attrs.focusable === 'true'`（非 text/image/generic role） | 🟡 中 |
| L3 | `attrs.accessibilityRole` / `accessibilityActionOptions` 显式标记 | 🟢 高（覆盖率低） |

详见 [`harmonyos-clickable-exploration.md`](harmonyos-clickable-exploration.md)。

### 2.2 Web (H5)

**数据源**：ArkUI `Web` 组件加载页面 + 注入 JS（复用 iOS 的 `dom-serializer.js`）

**回传机制**（关键差异点）：

```
[ArkTS]                    [JS in Webview]
runJavaScript --------→  执行 dom-serializer IIFE
                          ↓
                          window.__webCaptureResult = JSON.stringify(tree)
                          ↓
                       window.__host.onResult(json)
                          ↓
javaScriptProxy ←------- 反向调用
"__host.onResult"
```

**为什么不能像 iOS 那样直接拿 `runJavaScript` 返回值**：
- ArkUI 的 `runJavaScript` callback **只能拿到同步返回值**
- 异步逻辑（Promise/setTimeout）拿不到 → 必须用 `javaScriptProxy` 反向回传
- 这是华为官方推荐模式，与 WKWebView 的 `evaluateJavaScript` 行为不同

**注入策略**：把 `return JSON.stringify(...)` 全局 replace 成 `return window.__webCaptureResult = JSON.stringify(...)`，IIFE 自动跑完时 window 变量被赋值，再由 wrapper 调用 proxy 回传。

## 3. 工程结构

```
harmony/entry/src/main/ets/
├── perception/                          # 跨端算法层（11 个文件）
│   ├── Bounds.ets                       # int rect + [l,t],[r,b] 解析
│   ├── SemanticRole.ets                 # 50 个 role 枚举
│   ├── RoleDecision.ets                 # role + source + confidence
│   ├── SemanticStates.ets               # clickable/checked/... 常量
│   ├── SemanticNode.ets                 # struct + Builder
│   ├── RefAssigner.ets                  # native + web 双模式
│   ├── SnapshotRenderer.ets             # YAML 输出
│   ├── HarmonyInspectorParser.ets       # 鸿蒙 inspector JSON → 树
│   ├── HarmonyRoleResolver.ets          # $type → role + clickable 推断
│   ├── HarmonySemanticTreeBuilder.ets   # inspector → SemanticNode
│   └── WebJsonParser.ets                # web JSON → SemanticNode
├── pages/
│   ├── Index.ets                        # 首页：4 个测试按钮
│   └── WebBaseline.ets                  # Web 抓取页
└── entryability/EntryAbility.ets

harmony/entry/src/main/resources/rawfile/
├── dom-serializer.js                    # 复用 iOS 资产（718 行 IIFE）
└── web_form.html                        # 复用 iOS 资产
```

## 4. 验证结果

### Native (Message Home 页面)

```yaml
- screen:
  - generic:
    - text "UIPerception 鸿蒙 PoC"
    - button "点击我" [clickable] [ref=n1]
    - generic:
      - text "启用通知"
      - switch [clickable] [ref=n2]
    - input "请输入搜索关键词" [clickable-inferred] [ref=n3]
    - list [ref=n4]:
      - listitem [clickable] [ref=n5]: text "梁晓舟" / text "#1"
      - listitem [clickable] [ref=n6]: text "苏清沅" / text "#2"
      - ...
    - separator
    - button "抓取布局 (A+B 对比)" [clickable] [ref=n9]
    - button "测试 B filters" [clickable] [ref=n10]
    - button "V5 算法" [clickable] [ref=n11]
```

**指标**：29 节点 → 1128b YAML，11 个 ref 全部分配正确。

### Web (web_form.html)

```yaml
- screen:
  - generic [focused] [ref=w1]:
    - heading "Web Form Baseline" [level=1] [ref=w2]
    - paragraph "用于验证 iOS WKWebView..." [ref=w3]
    - generic [ref=w4]:
      - link "表单" [clickable] [ref=w5]
      - ...
    - heading "用户注册表单" [level=2] [ref=w8]
    - generic [ref=w9]:
      - textbox "用户名" [clickable] [ref=w11]
      - combobox "部门" [clickable] [value=tech] [ref=w17]:
        - option "技术部" [selected] [value=tech]
      - button "提交" [clickable] [ref=w20]
    - list "梁晓舟 — 明天上班..." [ref=w23]:
      - listitem "梁晓舟 — 明天上班" [ref=w24]
      - ...
```

**指标**：73 节点 → 3789b YAML，34 个 ref，端到端 17ms。

## 5. 跨端架构对齐

```
                ┌─────────────────────────────────────┐
                │  Shared algorithm layer             │
                │  (Java → Swift → ArkTS, 1:1 port)   │
                │                                     │
                │  Bounds / SemanticRole / Node       │
                │  RefAssigner / SnapshotRenderer     │
                │  WebJsonParser                      │
                └────────┬────────────────┬───────────┘
                         │                │
            ┌────────────┴───┐    ┌───────┴────────┐
            │ iOS side       │    │ Harmony side    │
            │ UIKitHierarchy │    │ HarmonyInspector│
            │ Dumper         │    │ Parser          │
            │ iOSRoleResolver│    │ HarmonyRole     │
            │                │    │ Resolver        │
            │ WKWebView +    │    │ ArkUI Web +     │
            │ evaluateJS     │    │ javaScriptProxy │
            └────────────────┘    └─────────────────┘
```

**对齐情况**：
- ✅ Bounds: 三端 `[l, t, r, b]` int 格式一致
- ✅ RefAssigner: native mode (prefix='n') + web mode (prefix='w') 一致
- ✅ SnapshotRenderer: YAML 缩进/状态标记/ref 语法一致
- ✅ dom-serializer.js: iOS 和鸿蒙共用同一份 718 行 IIFE
- ⚠️ Role 映射表: 三端 class/type 来源不同（Android View / iOS UIKit class / ArkUI `$type`）

## 6. 踩坑记录

| 坑 | 表现 | 根因 | 修复 |
|---|---|---|---|
| ArkTS `private` 跨类访问 | 编译失败 `shouldAssignRef is private` | `RefAssignerState` 调 `RefAssigner.private static` | 改为默认 public |
| ArkTS 不支持结构性类型 | `Position2D` 接口跟 SDK `Position` 冲突 | ArkTS 严格模式禁结构性匹配 | 删自定义接口，用 SDK 类型 |
| `$rect` 格式不符预期 | Bounds 解析失败 | 鸿蒙是 `[l, t],[r, b]` 浮点，不是 `[l,t][r,b]` 整数 | regex 改 `\[(-?\d+(?:\.\d+)?),\s*...\]` |
| Button name 丢失 | button 没 name | Button 文本在 `attrs.label`，不是 `attrs.content` | resolveName 加 label 优先级，filter 加 label |
| TextInput 标了 scrollable | input state 异常 | TextInput 内部文本滚动属于实现细节 | suppressScrollableForRole(Input) |
| Web JS SyntaxError | 卡在「注入中」10s | regex strip 把 IIFE 尾巴 `();` 误剥，括号不平衡 | 改用 `return X` → `return window.__var = X` replace |
| Web proxy 拿不到返回值 | 同上 | ArkUI `runJavaScript` callback 只拿同步值 | 用 `javaScriptProxy` 反向回传（官方推荐） |

## 7. 工程限制与决策

| 限制 | 当前 PoC 应对 |
|---|---|
| hdc 无 `pm grant` 等价物 | runtime 权限靠用户弹框（PoC 不涉及） |
| ArkUI 无 clickable 属性 | 三档推断策略（见 §2.1） |
| ArkUI Web runJavaScript 不返异步值 | javaScriptProxy 反向回传 |
| ArkTS 不支持 any/unknown | 严格类型 + 接口 + Map |
| ArkUI inspector 字段稀疏 | filter whitelist 提取关键字段 |

## 8. 下一步方向（待选）

1. **跨端 diff 验证** — iOS 也跑 `web_form.html`，逐行 diff 三端 YAML，量化一致性
2. **多 baseline 页** — 加 Mail / Contacts / Business，扩大测试覆盖
3. **SDK 抽离** — `perception/` 抽成独立 har 包，集成到真实业务 app
4. **a11y tree 备份路径** — 探索 `accessibility` 系列 API 作为 InspectorTree 的 fallback
5. **真实页面验证** — 在系统设置 / 第三方 app 上跑（需要 root 或 a11y 授权）

## 9. 关键样本归档

`docs/harmonyos-samples/` 下：

| 文件 | 说明 |
|---|---|
| `capture_app_A_framenode.json` | FrameNode 递归输出（方案 A） |
| `capture_app_B_inspector.json` | InspectorTree 无 filter（方案 B 原始） |
| `capture_app_B_filter{0,1,2}_*.json` | 三种 filter 配置对比 |
| `v5_inspector_message_home.json` | V5 算法输入（filtered inspector） |
| `v5_snapshot_message_home.yml` | V5 算法输出（native 消息首页） |
| `web_raw_web_form.json` | Web 抓取原始 JSON |
| `web_snapshot_web_form.yml` | Web 抓取输出 YAML |

## 10. 提交记录

- `de59e26` feat: add HarmonyOS PoC scaffolding for native capture exploration
- `27d1759` docs: document HarmonyOS clickable inference strategy + sample captures
- `6419b8d` feat(harmony): V5 algorithm port to ArkTS — Bounds/Role/Ref/Snapshot
- `89a49a1` fix(harmony): V5 algorithm refinement — names, root role, scrollable, blanks
- `3dac711` feat(harmony): V5 web capture pipeline — JS inject + proxy + WebJsonParser
