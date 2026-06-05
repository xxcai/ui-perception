# ADB HTTP 调试指南

通过 ADB 端口转发，从电脑端直接调用设备上的 Perception SDK HTTP 服务。

## 连接

```bash
# 建立端口转发（PC 9700 → 设备 9700）
adb forward tcp:9700 tcp:9700

# 验证连通性
curl http://localhost:9700/ping
```

返回：
```json
{"status":"success","result":{"version":"1.0.0"}}
```

## 抓取

### `/capture` — 完整语义快照

```bash
curl http://localhost:9700/capture
```

返回融合后的 YAML 文本（native + web 自动拼接）：
```json
{
  "status": "success",
  "result": {
    "activity": "MainActivity",
    "yaml": "- screen:\n  - toolbar ...",
    "timestamp": 1717500000000
  }
}
```

### `/capture-raw` — 原始抓取数据

```bash
curl http://localhost:9700/capture-raw
```

返回各插件的原始抓取结果（未经 transform/融合）：
```json
{
  "status": "success",
  "result": {
    "activity": "MainActivity",
    "plugins": [
      {"plugin": "native", "contentType": "application/xml", "content": "<hierarchy>..."},
      {"plugin": "web", "contentType": "application/json", "content": "{...}"}
    ]
  }
}
```

## 操作

操作端点依赖 `/capture` 建立的 ref→bounds 缓存。**必须先调用 `/capture`，再执行操作。**

### `/click` — 点击

```bash
curl -X POST http://localhost:9700/click \
  -H "Content-Type: application/json" \
  -d '{"ref": "n1"}'
```

### `/long_press` — 长按

```bash
curl -X POST http://localhost:9700/long_press \
  -d '{"ref": "n1", "duration": 1000}'
```

`duration` 可选，默认 500ms。

### `/swipe` — 滑动

```bash
# 从屏幕中心向上滑
curl -X POST http://localhost:9700/swipe -d '{"direction": "up"}'

# 从指定元素位置滑
curl -X POST http://localhost:9700/swipe -d '{"direction": "down", "ref": "w3"}'
```

direction: `up` / `down` / `left` / `right`

### `/type_text` — 输入文本

```bash
curl -X POST http://localhost:9700/type_text \
  -d '{"ref": "w3", "text": "hello", "clear": true}'
```

`clear` 可选，默认 `true`（先清空再输入）。

### `/check` / `/uncheck` — 切换选中状态

```bash
curl -X POST http://localhost:9700/check -d '{"ref": "n2"}'
curl -X POST http://localhost:9700/uncheck -d '{"ref": "n2"}'
```

### `/select_option` — 选择下拉选项（仅 web）

```bash
curl -X POST http://localhost:9700/select_option \
  -d '{"ref": "w1", "value": "option1"}'
```

### `/press_key` — 按键

```bash
curl -X POST http://localhost:9700/press_key -d '{"key": "back"}'
```

key: `back` / `enter` / `tab` / `home` / `menu`（home/menu 受系统限制，仅 back 可靠生效）

## 调试

### `/exec_js` — 在 WebView 中执行 JS

```bash
curl -X POST http://localhost:9700/exec_js \
  -d '{"js": "document.querySelectorAll(\"a\").length"}'
```

返回 JS 执行结果：
```json
{"status": "success", "result": "42"}
```

### `/debug-dom` — 获取原始 DOM 结构

```bash
curl http://localhost:9700/debug-dom
```

## 典型工作流

```bash
# 1. 转发端口
adb forward tcp:9700 tcp:9700

# 2. 抓取页面快照，查看可操作元素和 ref
curl -s http://localhost:9700/capture | python3 -m json.tool

# 3. 点击某个元素
curl -X POST http://localhost:9700/click -d '{"ref": "n3"}'

# 4. 重新抓取，验证页面变化
curl -s http://localhost:9700/capture | python3 -m json.tool
```

## 错误响应

所有端点统一返回格式：
```json
{"status": "error", "error": "错误信息"}
```

常见错误：
- `"Not found: /xxx"` — 路径不存在
- `"Missing body"` — POST 端点未发送请求体
- `"No dominant WebView found"` — 当前页面无 WebView（web 操作）
- `"Ref not found: n99"` — ref 未在缓存中（需要先 `/capture`）
- `"JS evaluation timed out"` — WebView JS 执行超时
