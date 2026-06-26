# HarmonyOS 预研规划 — 要验证什么

> 状态：Phase A 已完成，待决策是否进入 Phase B
> 前序：iOS PoC 已跑通 Native + Web capture 真机验证，跨端算法链路（RefAssigner / SnapshotRenderer / dom-serializer.js）已稳定
> 目标：判断 HarmonyOS 上能否复刻 Android/iOS 的 capture + agent bridge 能力，识别平台特有限制

## 0. 实测进度（Phase A 已完成）

| 验证项 | 状态 | 关键证据 |
|---|---|---|
| DevEco + hdc 环境 | ✅ | hdc 3.2.0d，DevEco Studio + 鸿蒙 SDK 已装；fish config 已配 PATH |
| 模拟器连通 | ✅ | `hdc list targets` → `127.0.0.1:5555` |
| **V2-b: `uitest dumpLayout` 路径** | ✅ **完全可行** | home 屏幕 60KB JSON / 86 节点 / 19 类型 / 26 字段；bounds `[l,t][r,b]` 与 Android 一致；中文 UTF-8 完整 |
| `hdc fport` 端口转发 | ✅ | `hdc fport tcp:9700 tcp:9700` 建立映射；`nc -z localhost 9700` 验证隧道通 |
| hdc 命令覆盖度 | 🟡 大部分对齐 adb | bm/aa/param/hilog/snapshot_display 都有；**缺 `pm grant` 等价物**（见下） |

### 关键发现 1：V2-b（`uitest dumpLayout`）数据质量优秀

样本：`docs/harmonyos-samples/layout_home.json`（home 屏幕）

字段密度对比 Android `uiautomator dump`：

| 字段类别 | Android | 鸿蒙 V2-b | 评价 |
|---|---|---|---|
| 节点类型 | `class` 全限定名 | `type`（Stack/Row/Text/Image/List/...） | ArkUI 类型更精简 |
| 坐标 | `[l,t][r,b]` | `[l,t][r,b]` | ✅ 字符串格式完全一致 |
| 文本 | `text` | `text` + `originalText` | ✅ |
| 描述 | `content-desc` | `description` | ✅ |
| 状态 | clickable/enabled/scrollable | clickable/enabled/scrollable + checked/selected/longClickable/checkable | 🟢 鸿蒙字段更多 |
| 标识符 | `resource-id` | `id` + `key` + `accessibilityId` | 🟢 三层标识 |

**含义**：V2-b 输出可直接对接现有 SemanticNode，TreeNormalizer 适配工作量极小（主要改 type 名表：`Stack → generic` / `Text → text` 等）。

### 关键发现 2：runtime 权限授予无 hdc 等价物 🔴

| Android | 鸿蒙 |
|---|---|
| `adb shell pm grant <pkg> <perm>` | ❌ 不存在 |
| `adb shell pm list permissions -d` | 🟡 `hdc shell atm dump -d` 列权限定义（≠ 实际授权状态） |
| `adb shell dumpsys package <pkg>` 看 reqPermissions | ✅ `hdc shell bm dump -n <pkg>` 输出 JSON 含 `reqPermissions` |

**已尝试的鸿蒙 grant 命令**（均不存在）：`acmd` / `pm` / `perm` / `permission` / `atool` / `scmd`

**`atm`（Access Token Manager）只有 dump，没有 grant 子命令**

**含义**：
- App 仍可通过 ArkTS API `requestPermissionsFromUser()` 触发**系统授权弹框**（需用户点一下，仅首次）
- 但无法像 Android 那样 agent bridge 后台静默授权
- 对 Perception SDK 影响有限：capture 路径（V2-b / V4）**不需要任何 user_grant 权限**；a11y 路径需要用户去设置页开启（与 Android 一致）

**对架构的影响**：Capture + Agent Bridge 主路径不阻塞；如果未来要支持「agent 自动跑流程时静默授权相机/位置」，鸿蒙做不到，需要用户体验妥协。



## 1. Context

iOS PoC 验证了三平台架构（L1 跨平台算法共享 / L2 半共享 / L3 平台重写）的可行性。HarmonyOS 作为第三个目标平台，需要在动用工程资源之前回答：**ArkUI 声明式 UI 框架能否被外部遍历**，以及由此带来的架构形态变化。

### 风险聚焦（基于公开资料 + 社区项目反向工程）

经预研检索，主要风险集中在**鸿蒙原生页面 capture**，原因：

1. **ArkUI 是声明式**：开发者「不能直接持有组件实例」，框架运行时管理组件树 —— 与 UIKit（递归 subviews）/ Android View（递归 getChildAt）命令式根本不同
2. **Web capture 跨端同构**：JS 注入 + dom-serializer.js 复用，鸿蒙差异仅在 `runJavaScript` vs `evaluateJavaScript` API 命名，风险低
3. **Agent Bridge 大部分可复用 Android 模式**：hdc ≈ adb（命令对照表已建立），`hdc fport` 端口转发机制等价，无需 iOS LAN 那种绕路

因此**本规划以 V2（ArkUI 树抓取）为核心**，V4/V6/V7/V8 降为 smoke test 级别验证。

预研产出**不是**完整 SDK，而是「能不能做」+「关键限制清单」+「建议实施路径」。

## 2. HarmonyOS 平台认知（基于公开资料）

| 维度 | HarmonyOS 现状 |
|---|---|
| OS 版本 | HarmonyOS NEXT（5.0+）纯血，移除 AOSP 兼容；4.x 及以下有 Android 兼容层 |
| 语言 | ArkTS（TypeScript 超集，强类型） |
| UI 框架 | ArkUI（声明式，`@Component struct` + 装饰器） |
| IDE | DevEco Studio（基于 IntelliJ） |
| 调试工具 | **`hdc`**（HarmonyOS Device Connector），adb 等价物，支持 `hdc fport` 端口转发 |
| WebView | ArkUI `Web` 组件，JS 注入方法叫 `runJavaScript()`（不是 `evaluateJavaScript`） |
| 无障碍 | **`AccessibilityExtensionAbility`**，ExtensionAbility 形态，跨 App AX 树 |
| UI 测试 | `arkXtest`（npm 包名 `hypium`），含 JsUnit + UiTest，类似 XCUITest |
| 官方自动化替代 | `hmdriver2`（Python，社区），绕开 hypium |

## 3. 要验证的核心问题

按风险分级重组。**V2 是决定鸿蒙端架构形态的关键问题**，其余 V4/V6/V7/V8 为 smoke test。

---

### 🔴 V2. ArkUI 组件树能否被遍历（**核心问题，最大不确定性**）

**问题**：ArkUI 是声明式（`@Entry @Component struct`），运行时组件树能不能像 UIView 树那样递归 walk？

#### 已查明的事实

基于公开资料 + 社区项目（hmdriver2 / appium-harmonyos-driver）反向工程：

| 候选路径 | 可行性 | 备注 |
|---|---|---|
| **`@ohos.arkui.inspector`** | 🔴 私有/系统 API（API 11+） | DevEco Inspector 用它；三方 App 拿不到。dev signing 是否能绕过需实测 |
| **`uitest dumpLayout` shell 命令** | 🟢 已被社区项目验证可用 | hmdriver2 / appium-harmonyos-driver 都走这条；从 App 外（host via hdc shell）调用，输出可对接 |
| **arkXtest `Driver.findComponent`** | 🟡 仅在测试 HAP 内可用 | 测试包形态可用，普通 App 不行；底层走 RPC 到系统 `uitest` 服务 |
| **AccessibilityExtensionAbility** | 🟢 公开 API | 看到的是 AX 树（被系统过滤），与 UIView 树差距类似 iOS |
| **CAPI native (`OH_ArkUI_NodeContent_*`)** | 🟡 仅混合开发场景 | 拿到的是 native 节点，不是 ArkTS 组件；纯 ArkTS App 不适用 |
| **App 内直接持有组件实例** | 🔴 不可行 | ArkUI 声明式语义决定 —— 框架管理组件树，开发者无法持有引用 |

**关键洞察**：ArkUI 声明式 = 开发者**不能持有组件实例**。这与 UIKit（`view.subviews` 递归）和 Android View（`view.getChildAt(i)` 递归）形成根本性差异。即使能在 App 内调某些 API，拿到的也不是「组件对象」而是「组件描述」。

#### PoC 验证步骤（按优先级递减）

1. **`@ohos.arkui.inspector` 实测**（V2-a）
   - 在 dev-signed HAP 里 `import inspector from '@ohos.arkui.inspector'`
   - 看编译能否通过、运行时是否抛权限错误
   - 如果通了：能拿到 ArkUI 树吗？字段密度如何？
   - **如果通** → in-app capture 路线成立，与 iOS/Android 同构

2. **`uitest dumpLayout` 外部 dump 路径**（V2-b，**重点验证**）
   - `hdc shell uitest dumpLayout` 跑一次，拿到输出
   - 解析输出格式（XML？JSON？），与现有 SemanticNode 字段对照
   - 这条路通了意味着鸿蒙架构**改为 host-driven**：App 不抓，host 通过 hdc shell 抓
   - **如果通** → 架构与 iOS/Android 都不同，但可行

3. **AccessibilityExtensionAbility dump**（V2-c，兜底）
   - 写一个 `AccessibilityExtensionAbility` 模块
   - 在目标 App 前台时调 `getRootElement()` / `getWindows()`
   - dump 整棵 AX 树到 JSON
   - 与 iOS UIKit 树输出做字段对比
   - **如果通** → 类似 iOS WDA 评估时的 AX 树路线，但跨 App

#### 通过标准（任一即可）

- V2-a 通：dev-signed App 内能拿到完整 ArkUI 树，至少含 type/text/bounds/clickable/enabled
- V2-b 通：host via hdc shell 能拿到 dump，字段密度足够支撑 LLM agent
- V2-c 通：AX 树兜底，至少含 role / name / bounds / clickable / enabled

#### 架构影响（决定后续 Phase D 形态）

| 哪条路通 | 鸿蒙端架构 |
|---|---|
| V2-a | 与 iOS/Android 同构（in-app HTTP server + ref→bounds） |
| V2-b | **host-driven**：host 通过 hdc shell 抓取，App 不需要内置 capture 逻辑 |
| V2-c | 类似 iOS WDA（AX 树），但 in-app；ref 系统需适配 |

**风险等级**：🔴 高 —— 直接决定鸿蒙端架构形态。**这是本次预研的主要工作**。

---

### 🟡 V3. AccessibilityExtensionAbility AX 树字段密度（V2-c 的前置验证）

**问题**：作为 V2 的兜底，AX 树输出长什么样、信息密度够不够？

**注意**：如果 V2-a 或 V2-b 通，V3 不必深做，仅需快速验证字段是否齐全。

**验证步骤**：
- 写一个 `AccessibilityExtensionAbility` 模块
- 在目标 App 前台时调 `getRootElement()` / `getWindows()`
- dump 整棵 AX 树到 JSON
- 与 iOS UIKit 树输出做字段对比：role / name / bounds / states 是否齐全

**通过标准**：能拿到完整 AX 树，至少包含 role / name / bounds / clickable / enabled，可对接现有 SemanticNode
**注意**：AX 树天然丢失中间容器节点（与 iOS 评测时一致），TreeNormalizer 需要适配

---

### 🟢 V4. WebView + runJavaScript + dom-serializer.js（smoke test）

**问题**：把 dom-serializer.js 灌进 ArkUI `Web` 组件，能不能正常跑、输出格式与 iOS/Android 一致？

**风险等级**：🟢 低 —— JS 注入 + 同一份 dom-serializer.js，跨端同构。

**验证步骤**（精简版）：
- ArkTS 写一个 `Web` 组件加载 `web_form.html`（已存在）
- 调 `webView.runJavaScript(domSerializerJs)` 拿返回值
- 同一 HTML 输出与 iOS 真机已验证结果 byte-level 对比

**通过标准**：
- `runJavaScript` 能拿到 dom-serializer.js 返回的 JSON 字符串
- 输出与 iOS/Android 一致
- dom-serializer.js 无需改动

**坑预警**（仅文档化，不做预攻）：
- `runJavaScript` 回调签名（async vs sync）
- 异步 callback 是否在主线程派发

---

### 🟢 V5. 跨端算法移植到 ArkTS（L1 层验证）

**问题**：RefAssigner / SnapshotRenderer / TreeNormalizer 用 ArkTS 重写，工作量与 Swift 比如何？

**预期**：ArkTS 是 TS 超集，比 Swift 更接近 Kotlin（Java 源），移植应该比 iOS 更快。

**验证步骤**（精简版）：
- 选 `Bounds.swift`（最简单）和 `RefAssigner.swift`（含 listitem 三档逻辑）
- 用 ArkTS 重写
- 喂同一个 SemanticNode 树，对比输出

**通过标准**：
- 算法逻辑 1:1 移植成功
- 单元测试输出与 Swift 一致
- 工作量 < 1 天

---

### 🟢 V6. HTTP server in ArkTS（Agent Bridge 基础，smoke test）

**问题**：ArkTS 能不能起一个 HTTP server 监听端口？

**风险等级**：🟢 低 —— 若 V2-b 路线成立（host-driven），此项可跳过。

**验证路径**：
- ArkTS 标准 API：`@ohos.net.http` 是客户端；服务端用 `@ohos.net.socket`（TCP socket）
- 写最小 HTTP server：监听 9700，返回 `{version: "1.0.0-harmony"}`
- hdc fport 转发后，Mac 上 `curl localhost:9700/ping` 能通

**通过标准**：
- 用 `@ohos.net.socket` 起一个 HTTP listener
- 真机上 9700 端口能被 Mac 通过 `hdc fport tcp:9700 tcp:9700` 访问

**关键差异 vs iOS**：鸿蒙有 hdc fport（公开 API），iOS 沙箱禁止 —— **这是鸿蒙相对 iOS 的最大优势**

---

### 🟢 V7. Agent Operations 可行性（smoke test）

**问题**：click / type / swipe 这套操作，鸿蒙上能不能在 App 进程内执行？

**风险等级**：🟢 中低 —— 鸿蒙沙箱比 iOS 宽松（hdc fport 说明），但 ArkUI 是否暴露组件动作 API 未知。

**关键判断**：
- 路径 A（in-app）：如果 V2-a 通，组件实例拿不到，in-app 操作很可能也不通
- 路径 B（host-driven via uitest）：如果 V2-b 通，操作也走 `hdc shell uitest ...`，App 不参与
- 路径 C（hypium 测试包）：作为兜底

**通过标准**：至少 3 个核心操作（click / type / swipe）有可行实现路径，文档化各路径优缺点

**注意**：此项验证依赖 V2 结论。如果 V2-b 路线成立，V7 几乎自动有解（跟随 V2-b）。

---

### 🟢 V8. hdc 工具链 vs adb（对照表，无独立验证）

**问题**：hdc 是否覆盖 adb 的所有常用功能？

**对照表**：

| 操作 | adb | hdc |
|---|---|---|
| 列设备 | `adb devices` | `hdc list targets` |
| 装应用 | `adb install x.apk` | `hdc install x.hap` |
| shell | `adb shell` | `hdc shell` |
| 端口转发 | `adb forward tcp:X tcp:X` | `hdc fport tcpn:X tcpn:X` |
| 推文件 | `adb push` | `hdc file send` |
| 截屏 | `adb shell screencap` | `hdc shell snapshot_display` |
| **UI dump** | `adb shell uiautomator dump` | **`hdc shell uitest dumpLayout`** |

**关键差异**：`uitest dumpLayout` 是鸿蒙特有的 UI 树 dump 命令，对应 V2-b 路径。**这是潜在架构差异点**。

**通过标准**：覆盖 80%+ 常用 adb 操作（与现有 Android agent bridge host 客户端兼容）

## 4. 验证阶段顺序（按风险优先级重排）

### Phase A — 环境与命令行（V1 + V8）— 0.5 天

最先做。如果 DevEco / hdc 装不起来，后面全废。

**产出**：
- 一份「环境搭建 cheatsheet」（Mac 路径）
- hdc vs adb 命令对照表
- **顺手做**：跑一次 `hdc shell uitest dumpLayout`，把输出文件保留下来供 Phase B 分析

**Stop / Go 判定**：能跑 Hello World 到真机 + hdc fport 通 → 进 Phase B

---

### Phase B — ArkUI 树抓取深挖（V2 + V3）— 2-3 天 ⭐ **核心阶段**

**为什么是最重要的阶段**：决定鸿蒙端整体架构形态（in-app vs host-driven vs AX 树）。

**子阶段**：

**B.1 — `@ohos.arkui.inspector` 实测**（V2-a，0.5 天）
- dev-signed HAP import 私有模块
- 记录编译/运行错误
- 如果通：dump 出树，分析字段

**B.2 — `uitest dumpLayout` 路径深挖**（V2-b，1 天）⭐ **重点**
- 解析 dumpLayout 输出格式（XML/JSON）
- 写一个 ArkTS 或 host 端解析器，对接现有 SemanticNode
- 验证：鸿蒙复刻 MessageHome 页面，dump 出来字段是否齐全
- 评估：延迟、稳定性、是否需要 root / 特殊权限

**B.3 — AccessibilityExtensionAbility 路径**（V2-c / V3，1 天）
- 写 AX ExtensionAbility
- dump 整棵 AX 树
- 与 V2-a / V2-b 输出对比字段密度

**产出**：
- 三条路径的可行性报告
- 推荐 architecture（in-app / host-driven / AX）
- 同一 baseline 页面的 native capture YAML 输出（至少一条路径）

**Stop / Go 判定**：
- 任一路径通 → 进 Phase C
- 都失败 → 鸿蒙 in-app capture 不可行，宣告不可行或考虑 hypium 测试包路线

---

### Phase C — WebView + 跨端算法（V4 + V5）— 1 天

**为什么放第三**：风险最低、收益直接。WebView + JS 是 Android/iOS 已验证的成熟链路；ArkTS 移植接近 TS。即使 V2 失败，这块也能跑通。

**产出**：
- `web_form.html` 在鸿蒙 Web 组件加载，capture 输出 YAML
- ArkTS 版 `Bounds` / `RefAssigner` 单元测试通过
- 一份「ArkTS vs Swift 移植工作量对比」

**Stop / Go 判定**：WebView capture 输出与 iOS 对齐 + 算法移植成功 → 进 Phase D

---

### Phase D — Agent Bridge 最小闭环（V6 + V7）— 1-2 天

**形态依赖 Phase B 结论**：

- **若 V2-a 通**（in-app）：与 iOS/Android 同构，写 HTTP server + ref→bounds + in-app ops
- **若 V2-b 通**（host-driven）：host 端通过 hdc shell 调度，App 仅做最小协作（如打开页面、回包 ping）
- **若 V2-c 通**（AX 树）：类似 iOS WDA 路线，但 in-app

**产出**：
- 端口 9700 的 HTTP server（或等价的 host-driven 调度器）
- ping + capture + click + type 四个端点
- Mac 上 `curl` 或 `hdc shell` 能驱动鸿蒙真机
- 一份「鸿蒙 vs iOS Agent Bridge 对比」（特别说明 hdc fport 让鸿蒙更顺利）

## 5. 整体通过标准（PoC 完成判定）

### 必须达成（硬指标）

- ✅ DevEco + 真机部署链路通
- ✅ **V2 至少一条路径通**（V2-a / V2-b / V2-c 任一）—— 这是核心
- ✅ WebView capture 输出与 iOS/Android byte-level 一致
- ✅ ArkTS 版 RefAssigner 单元测试通过

### 加分项

- ✅ hdc fport + HTTP server + click 端到端通
- ✅ operations (click/type) 在 App 内或 host 端可调用
- ✅ `@ohos.arkui.inspector` 私有 API 可用（dev signing），记录为正式版风险

### 失败定义

- V2 三条路径都失败 → 鸿蒙 SDK 不可行（**最坏情况**，但 `uitest dumpLayout` 已被社区项目验证，可能性低）
- ArkTS 移植工作量 > 5 天 → 跨端架构有鸿沟，需重新评估

## 6. 关键技术参考

### HarmonyOS 官方

- [ArkUI WebView 组件](https://developer.huawei.com/consumer/cn/forum/topic/0201131497920921052) — `runJavaScript()` 用法
- [AccessibilityExtensionAbility](https://developer.huawei.com/consumer/en/doc/harmonyos-references/js-apis-application-accessibilityextensionability-V13) — AX 树查询入口
- [UiTest 用户指南](https://developer.huawei.com/consumer/en/doc/harmonyos-guides/uitest-guidelines) — hypium 自动化测试
- [hdc 命令参考](https://gitee.com/openharmony/docs/blob/master/en/application-dev/dfx/hdc.md) — OpenHarmony 官方

### 社区（V2-b 路径反向工程的关键参考）

- [hmdriver2（Python 替代 hypium）](https://dev.to/believezj/automated-ui-testing-for-harmonyos-5-business-apps-using-python-and-hmdriver2-36m6) — **重点参考其 dumpLayout 解析**
- [appium-harmonyos-driver](https://github.com/appium/appium-harmonyos-driver) — 同上，对照实现
- [hdc-rs（含 port forwarding）](https://github.com/oslo254804746/hdc-rs)
- [arkXtest 实战](https://www.seaxiang.com/blog/093bade094ba4c79bb42016a9e1efadc)

### 项目内对齐参考

- [docs/ios-port-research.md](./ios-port-research.md) — iOS PoC 同类报告模板
- [docs/ios-agent-bridge-plan.md](./ios-agent-bridge-plan.md) — Agent Bridge 跨端对比
- [docs/ios-research-summary.md](./ios-research-summary.md) — 阶段总结模板
- Android dumper / web-plugin 源码 — 三端共享基准

## 7. 预估投入

| 阶段 | 工作量 | 关键产出 |
|---|---|---|
| Phase A | 0.5 天 | 环境 + hdc 命令表 + dumpLayout 样本 |
| **Phase B** | **2-3 天** | **ArkUI 树抓取三路径验证报告（核心）** |
| Phase C | 1 天 | WebView capture + 算法移植 |
| Phase D | 1-2 天 | Agent Bridge 最小闭环（形态依赖 B 结论） |
| **总计** | **4-6 天** | **完整的鸿蒙可行性报告** |

相比原规划（5-8 天）压缩 1-2 天，原因：用户指出 web / agent bridge 部分可复用 Android 经验，主要工作量集中在 V2（ArkUI 树抓取）。

实际投入取决于 V2-a（`@ohos.arkui.inspector` 在 dev signing 下能否用）。若 V2-a 直接通，整体可压缩到 3-4 天；若必须走 V2-b 重写 host 端，可能延长到 6 天。

## 8. 不在 PoC 范围

- 鸿蒙 4.x 及更早版本（带 AOSP 兼容层）—— 直接用 Android SDK 就行，不是本研究目标
- 鸿蒙手表 / 电视 / 车机等其他形态 —— 仅验证手机（phone）
- 跨设备分布式能力（鸿蒙卖点之一）—— 与 SDK 无关
- App Galore / 上架审核 —— PoC 阶段 dev signing 即可
- 性能压测 / 大型页面 benchmark —— iOS PoC 也没做，统一在正式 SDK 阶段补
- 与 HMS / Petal Search 等华为服务集成 —— 业务层，不属 SDK 底座

## 9. 与 iOS PoC 的对照预期

| 维度 | iOS PoC 实际 | 鸿蒙预期 |
|---|---|---|
| Native capture 难度 | UIView 树递归（5 天） | **未知，2-3 天验证**（V2 决定） |
| Web capture | 3 天 | 1 天（JS 跨端同构） |
| Agent Bridge 通信 | WiFi/LAN（iOS 沙箱卡死 USB 转发） | **hdc fport USB 转发**（明显更顺） |
| 触摸事件合成 | 私有 API 禁用，改 ref→UIView→sendActions | 待 V7 验证（若 V2-b 通则不是问题） |
| 主要风险 | evaluateJavaScript semaphore 死锁 | **ArkUI 树遍历 API 不公开**（V2） |

如果鸿蒙在 V2 上能避开 iOS 那两个大坑（私有 API + 沙箱），整个 Agent Bridge 实施会比 iOS 更顺利。但 V2 本身的不确定性是 iOS 没有的（iOS UIView 树是确定可遍历的）。

## 10. 架构决策树（基于 V2 结果）

```
V2 验证结果？
├── V2-a 通（@ohos.arkui.inspector dev 可用）
│   └── 架构 = in-app HTTP server + ref→bounds
│       └── 与 iOS/Android 同构，复用现有 host 客户端
│
├── V2-b 通（uitest dumpLayout 字段够）
│   └── 架构 = host-driven via hdc shell
│       └── host 端解析 dumpLayout，App 仅做协作
│       └── 类似 Android UiAutomator 路线，但更轻
│
├── V2-c 通（AX 树兜底）
│   └── 架构 = in-app AX dump + TreeNormalizer 适配
│       └── 类似 iOS WDA 路线，但 in-app 而非独立进程
│
└── 都失败
    └── 鸿蒙 SDK 不可行 / 或退到 hypium 测试包路线
```

**这个决策树是本次预研的核心产出。** Phase B 完成后必须给出明确推荐。

## 11. 下一步

按本规划启动 Phase A。第一个决策点：**是否有鸿蒙真机可用？**（V1 验证依赖真机；纯模拟器无法验证 hdc fport 与 USB 调试）

Phase A 完成后立即进入 Phase B（核心）。Phase B 的 B.2（`uitest dumpLayout`）是**最低风险有产出**的子任务，建议优先做。
