# HarmonyOS PoC — UIPerception

> 鸿蒙端 native capture 探索的目标 App。最小 ArkTS HAP，UI 含多种组件类型供 `uitest dumpLayout` / `@ohos.arkui.inspector` / AccessibilityExtensionAbility 三路径验证字段密度。

## 工程结构

```
harmony/
├── AppScope/                       # App 级配置
│   ├── app.json5                   # bundleName、版本
│   └── resources/base/element/     # app_name 字符串
└── entry/                          # 主 HAP 模块
    ├── src/main/
    │   ├── ets/
    │   │   ├── entryability/EntryAbility.ets   # UIAbility 入口
    │   │   └── pages/Index.ets                 # 唯一页面（含 Button/Toggle/TextInput/List）
    │   ├── resources/
    │   │   ├── base/
    │   │   │   ├── element/{string,color}.json
    │   │   │   ├── media/                      # 占位图标
    │   │   │   └── profile/main_pages.json     # 页面路由
    │   │   └── dark/                           # 暗色模式（占位）
    │   └── module.json5                        # 模块配置
    ├── build-profile.json5
    ├── hvigorfile.ts
    └── oh-package.json5
├── build-profile.json5             # 工程级（compatibleSdkVersion 5.0.0(12)）
├── hvigorfile.ts
└── oh-package.json5
```

## UI 组件清单（用于 capture 字段对照）

| 组件 | ArkTS 类型 | 期望 dumpLayout 字段 |
|---|---|---|
| 标题 | `Text` | text + bounds |
| 按钮 | `Button` | clickable + text |
| 开关 | `Toggle(Switch)` | checkable + checked |
| 输入框 | `TextInput` | editable（待验证） |
| 列表 | `List` | scrollable |
| 列表项 | `ListItem` × 4 | clickable-inferred |
| 副文本 | `Text` × 8 | text |

## 如何构建 / 部署

### 方式 A：DevEco Studio（推荐）

1. DevEco Studio → File → Open → 选 `harmony/` 目录
2. 等 Sync 完成（首次会下载 hvigor / ohpm 依赖）
3. 顶部 Device 下拉选已启动的模拟器
4. 点 ▶ Run → 等 HAP 编译 + 安装 + 启动

### 方式 B：CLI

```bash
cd harmony
# 首次：装依赖
ohpm install

# 构建
hvigorw assembleHap --mode module -p product=default

# 安装到设备
hdc install entry/build/default/outputs/default/entry-default-signed.hap

# 启动
hdc shell aa start -a EntryAbility -b com.hh.uiperception.harmony
```

## 验证 dumpLayout（capture 探索）

App 启动后跑：

```bash
hdc shell uitest dumpLayout
hdc file recv /data/local/tmp/layout_<ts>.json ./layout_app.json
```

期望输出含：`Button` / `Toggle` / `TextInput` / `List` / `ListItem` 等类型节点。对照 `docs/harmonyos-samples/layout_home.json`（home 屏幕样本）看 App 内字段密度差异。

## 不在 PoC 范围

- Agent Bridge HTTP server（待 Phase D）
- 算法层 ArkTS 移植（待 Phase C）
- AccessibilityExtensionAbility 模块（待 V2-c 验证）
- 多页面 / 导航（PoC 单页够用）

## 关联

- 总规划：[../docs/harmonyos-research-plan.md](../docs/harmonyos-research-plan.md)
- 已采集样本：[../docs/harmonyos-samples/](../docs/harmonyos-samples/)
- iOS 对照工程：[../ios/](../ios/)
