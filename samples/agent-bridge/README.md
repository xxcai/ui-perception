# Agent Bridge 示例

设备端 App（含 WebView）通过 JS Bridge + adb reverse 调用 PC 端 Agent 的通信方案。

## 架构

```
┌──────────────────────────────────────────────┐
│              Android 设备                     │
│                                              │
│  ┌──────────┐    JS Bridge    ┌───────────┐  │
│  │  WebView │ ──────────────> │   Native  │  │
│  │   (JS)   │ <────────────── │   Bridge  │  │
│  └──────────┘                 └─────┬─────┘  │
│                                     │        │
│                           HTTP      │        │
│                           localhost │        │
│                           :9800     ▼        │
└─────────────────────────────────────┼────────┘
                                      │
                                  adb reverse
                                      │
                                      ▼
                             ┌──────────────┐
                             │    Agent     │
                             │   (PC :9800) │
                             └──────────────┘
```

## 通信流程

1. Agent 启动 HTTP Server 监听 9800 端口
2. Agent 执行 `adb reverse tcp:9800 tcp:9800` 将设备端口映射到 PC
3. WebView 中 JS 调用 `window.AgentBridge.callAgent(task)`
4. Native 侧通过 `HttpURLConnection` 请求 `http://localhost:9800/task`
5. Agent 接收任务，处理后将结果通过 HTTP 响应返回

## 使用方式

### 启动 Agent 服务器

```bash
node agent-test-server.mjs
```

### 设置 adb reverse

```bash
adb reverse tcp:9800 tcp:9800
```

### API

**POST /task**

请求：
```json
{"task": "任务描述"}
```

响应：
```json
{"status": "success", "result": {"message": "Agent processed: ...", "timestamp": "..."}}
```

**GET /ping**

健康检查。

## App 侧集成要点

1. WebView 启用 JavaScript：`webView.getSettings().setJavaScriptEnabled(true)`
2. 注册 JS Bridge：`webView.addJavascriptInterface(new AgentBridge(), "AgentBridge")`
3. AgentBridge 中通过 `HttpURLConnection` 调用 `http://localhost:9800/task`
4. AndroidManifest 添加 `android:networkSecurityConfig` 允许 localhost 明文 HTTP

## 关键文件

| 文件 | 说明 |
|------|------|
| `agent-test-server.mjs` | Agent 测试服务器（Node.js，零依赖） |
| `WebViewTestActivity.java` | Android 端 WebView + JS Bridge 示例 |
| `network_security_config.xml` | 允许 localhost 明文 HTTP |
