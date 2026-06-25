# Issues

记录跨端 web/native capture 链路中发现的设计/实现问题。每个 issue 一个 markdown 文件，按编号递增。

## 当前列表

- [#001 — dom-serializer.js 不输出 ARIA props（href/placeholder）](./001-web-dom-serializer-missing-props.md)

## 约定

- 文件名：`NNN-简短标题.md`（三位数字）
- 字段建议：状态 / 影响 / 复现 / 根因 / 修复方案 / 验收 / 关联
- 根因一定要带源码位置（文件:行号）和参考实现（Playwright 源码路径）
- 修复方案跨端时，Java/Swift 两边都要列
- 闭环后把「状态」改成 Closed，但文件保留作为历史
