# UI Perception 文档

## 跨端 PoC 总览

三端（Android / iOS / HarmonyOS）的 PoC 总结，统一格式，相互对照：

- [android-poc-summary.md](android-poc-summary.md) — Android 基准实现（capture + agent bridge + touch dispatch 全生产可用）
- [ios-poc-summary.md](ios-poc-summary.md) — iOS PoC（Native + Web 跑通，Agent Bridge 设计完成未实施）
- [harmonyos-poc-summary.md](harmonyos-poc-summary.md) — HarmonyOS PoC（Native + Web 跑通，跨端算法 1:1 移植）

## 架构

- [architecture.md](architecture.md) — 项目整体架构与模块说明

## Android 专题

- [android-native-xml-transform-route.md](android-native-xml-transform-route.md) — Native XML → Semantic Snapshot 转换路线
- [adb-http-debugging.md](adb-http-debugging.md) — ADB HTTP 调试指南（端口转发 + 全部 ops 端点）
- [web-tree-optimization-backlog.md](web-tree-optimization-backlog.md) — Web Tree 优化 Backlog
- [playwright-comparison-report.md](playwright-comparison-report.md) — 与 Playwright 的对比报告

## iOS 专题

- [ios-port-research.md](ios-port-research.md) — iOS 端 Native Capture 调研报告
- [ios-agent-bridge-plan.md](ios-agent-bridge-plan.md) — iOS Agent Bridge 完整设计 + 7 个里程碑
- [ios-research-summary.md](ios-research-summary.md) — iOS 阶段总结（含 WDA 评估、Mac↔iOS 通信方案）

## HarmonyOS 专题

- [harmonyos-research-plan.md](harmonyos-research-plan.md) — 完整调研规划（V1-V8 验证矩阵 + Phase A-D）
- [harmonyos-clickable-exploration.md](harmonyos-clickable-exploration.md) — ArkUI clickable 推断策略（三档）

## 设计标准

- [llm-input-snapshot-standard.md](llm-input-snapshot-standard.md) — LLM Input Snapshot v1 统一输出标准
- [plugin-evaluation-integration.md](plugin-evaluation-integration.md) — 多插件评测接入说明

## Issues

- [../issues/001-web-dom-serializer-missing-props.md](../issues/001-web-dom-serializer-missing-props.md) — dom-serializer.js 不输出 ARIA props（跨端问题）

## 样本归档

- [harmonyos-samples/](harmonyos-samples/) — HarmonyOS 抓取样本（inspector tree / web JSON / V5 snapshot YAML）
