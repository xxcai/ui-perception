#!/usr/bin/env bash
#
# test-web-ops.sh — 端到端测试 Web 操作层
#
# 前置条件：
#   1. 设备已通过 adb 连接
#   2. APK 已安装（可用 --build 自动编译安装）
#
# 用法：
#   ./scripts/test-web-ops.sh           # 直接测试
#   ./scripts/test-web-ops.sh --build   # 先编译安装再测试
#

# ── 配置 ──────────────────────────────────────────────
PKG="com.hh.uiperception"
MAIN_ACTIVITY="${PKG}/.MainActivity"
PORT=9700
BASE_URL="http://localhost:${PORT}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ── 工具函数 ──────────────────────────────────────────
info()  { printf "${CYAN}[INFO]${NC} %s\n" "$*"; }
pass()  { printf "${GREEN}[PASS]${NC} %s\n" "$*"; }
fail()  { printf "${RED}[FAIL]${NC} %s\n" "$*"; }

# 直接 curl + python3 解析，避免大型 JSON 经过 shell 变量
api_status() {
  # 用法: api_status POST /click '{"ref":"w3"}'
  local method="$1" path="$2"
  shift 2
  if [ "$method" = "GET" ]; then
    curl -s "${BASE_URL}${path}" | python3 -c "
import sys, json
try:
    print(json.load(sys.stdin).get('status',''))
except:
    print('')
"
  else
    curl -s -X POST "${BASE_URL}${path}" \
      -H 'Content-Type: application/json' \
      -d "$@" | python3 -c "
import sys, json
try:
    print(json.load(sys.stdin).get('status',''))
except:
    print('')
"
  fi
}

api_field() {
  # 用法: api_field POST /select_option '{"ref":"w25","value":"banana"}' '.result.value'
  local method="$1" path="$2" body="$3" field="$4"
  if [ "$method" = "GET" ]; then
    curl -s "${BASE_URL}${path}" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    for p in '${field}'.lstrip('.').split('.'):
        if p: d = d[p] if isinstance(d, dict) else d[int(p)]
    print(d if isinstance(d, str) else json.dumps(d))
except:
    print('')
"
  else
    curl -s -X POST "${BASE_URL}${path}" \
      -H 'Content-Type: application/json' \
      -d "$body" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    for p in '${field}'.lstrip('.').split('.'):
        if p: d = d[p] if isinstance(d, dict) else d[int(p)]
    print(d if isinstance(d, str) else json.dumps(d))
except:
    print('')
"
  fi
}

# 从 capture 中提取指定 ref 的文本内容
capture_ref_text() {
  local ref="$1"
  curl -s "${BASE_URL}/capture" | python3 -c "
import sys, json, re
try:
    d = json.load(sys.stdin)
    for line in d['result']['yaml'].split('\n'):
        if '[ref=${ref}]' in line:
            m = re.match(r'\s*-\s+\w+\s+\"([^\"]+)\"', line.strip())
            if m: print(m.group(1))
            else: print('')
            break
except:
    print('')
"
}

wait_for() {
  local url="${BASE_URL}$1" max="${2:-10}"
  local i=0
  while [ $i -lt $max ]; do
    if curl -s "$url" >/dev/null 2>&1; then
      return 0
    fi
    i=$((i+1))
    sleep 1
  done
  return 1
}

# ── 前置准备 ──────────────────────────────────────────
FAILED=0

# 检查 adb 连接
if ! adb get-state >/dev/null 2>&1; then
  fail "No adb device connected"
  exit 1
fi

# 可选：编译安装
if [ "${1:-}" = "--build" ]; then
  info "Building APK..."
  ./gradlew :app:assembleDebug 2>&1 | tail -3
  info "Installing APK..."
  adb install -r app/build/outputs/apk/debug/app-debug.apk
fi

# 启动 app → 导航到 Web 测试页
info "Restarting app..."
adb shell am force-stop "$PKG"
sleep 1
adb shell am start -n "$MAIN_ACTIVITY" >/dev/null 2>&1
sleep 2

# 导航到 Web 测试页：滚动找到 "baseline://web/home" 对应的 "打开页面" 按钮
info "Navigating to Web baseline page..."

find_web_btn() {
  adb shell uiautomator dump /sdcard/ui_test.xml >/dev/null 2>&1
  adb shell cat /sdcard/ui_test.xml 2>/dev/null | python3 -c "
import sys, xml.etree.ElementTree as ET, re
try:
    tree = ET.parse(sys.stdin)
    nodes = list(tree.getroot().iter())
    for i, node in enumerate(nodes):
        if 'baseline://web/home' in node.get('text',''):
            for j in range(i+1, min(len(nodes), i+6)):
                if '打开页面' in nodes[j].get('text',''):
                    b = nodes[j].get('bounds', '')
                    nums = [int(x) for x in re.findall(r'\d+', b)]
                    print(f'{(nums[0]+nums[2])//2} {(nums[1]+nums[3])//2}')
                    break
            else:
                continue
            break
except:
    pass
" 2>/dev/null
}

# 最多尝试 5 次滚动查找
WEB_BTN=""
for attempt in 1 2 3 4 5; do
  WEB_BTN=$(find_web_btn || true)
  if [ -n "$WEB_BTN" ]; then
    break
  fi
  info "Scrolling down (attempt ${attempt})..."
  adb shell input swipe 540 1800 540 600 300
  sleep 1
done

if [ -z "$WEB_BTN" ]; then
  fail "Cannot find Web baseline card. Is the app showing the main page?"
  exit 1
fi

TAP_X=$(echo "$WEB_BTN" | awk '{print $1}')
TAP_Y=$(echo "$WEB_BTN" | awk '{print $2}')
info "Tapping Web card button at (${TAP_X}, ${TAP_Y})..."
adb shell input tap "$TAP_X" "$TAP_Y"
sleep 2

# 验证到达 Web 页面
CURRENT=$(adb shell dumpsys activity activities 2>/dev/null | grep "topResumedActivity" | head -1)
if echo "$CURRENT" | grep -q "WebBaselinePlaceholderActivity"; then
  info "Web test page opened"
else
  fail "Failed to open Web test page"
  exit 1
fi

# 等待 HTTP server 就绪
info "Waiting for HTTP server..."
if ! wait_for "/ping" 10; then
  fail "HTTP server not responding on port ${PORT}"
  exit 1
fi
info "HTTP server ready"

# ── 测试用例 ──────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════"
echo "  Web Operations E2E Test"
echo "═══════════════════════════════════════════════════"
echo ""

# --- Test 1: Capture ---
info "Test 1: Capture"
S=$(api_status GET /capture)
if [ "$S" = "success" ]; then
  pass "Capture returned success"
else
  fail "Capture returned: $S"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 2: Click ---
info "Test 2: Click (w3 - Click Me)"
S=$(api_status POST /click '{"ref":"w3"}')
if [ "$S" = "success" ]; then
  pass "Click API returned success"
else
  fail "Click API returned: $S"
  FAILED=$((FAILED+1))
fi

sleep 0.5
W4=$(capture_ref_text "w4")
if [ "$W4" = "Clicked!" ]; then
  pass "Click output shows 'Clicked!'"
else
  fail "Click output: expected 'Clicked!', got '$W4'"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 3: Type Text ---
info "Test 3: Type text (w6 - Type something input)"
S=$(api_status POST /type_text '{"ref":"w6","text":"hello world","clear":true}')
if [ "$S" = "success" ]; then
  pass "Type text API returned success"
else
  fail "Type text API returned: $S"
  FAILED=$((FAILED+1))
fi

# 关闭键盘（type_text 会弹出软键盘）
sleep 0.5
adb shell input keyevent KEYCODE_BACK
sleep 0.5
echo ""

# --- Test 4: Select Option ---
info "Test 4: Select option (w25 - select banana)"
# 重新 capture（键盘可能改变了布局）
api_status GET /capture >/dev/null
VAL=$(api_field POST /select_option '{"ref":"w25","value":"banana"}' '.result.value')
if [ "$VAL" = "banana" ]; then
  pass "Select option returned value=banana"
else
  fail "Select option: expected 'banana', got '$VAL'"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 5: Check ---
info "Test 5: Check (w14 - checkbox)"
api_status GET /capture >/dev/null
S=$(api_status POST /check '{"ref":"w14"}')
if [ "$S" = "success" ]; then
  pass "Check API returned success"
else
  fail "Check API returned: $S"
  FAILED=$((FAILED+1))
fi

sleep 0.5
W16=$(capture_ref_text "w16")
if [ "$W16" = "checked=true" ]; then
  pass "Checkbox output shows 'checked=true'"
else
  fail "Checkbox output: expected 'checked=true', got '$W16'"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 6: Uncheck ---
info "Test 6: Uncheck (w14 - checkbox)"
api_status GET /capture >/dev/null
S=$(api_status POST /uncheck '{"ref":"w14"}')
if [ "$S" = "success" ]; then
  pass "Uncheck API returned success"
else
  fail "Uncheck API returned: $S"
  FAILED=$((FAILED+1))
fi

sleep 0.5
W16_2=$(capture_ref_text "w16")
if [ "$W16_2" = "checked=false" ]; then
  pass "Checkbox output shows 'checked=false'"
else
  fail "Checkbox output: expected 'checked=false', got '$W16_2'"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 7: Radio ---
info "Test 7: Click radio (w19 - Option A)"
api_status GET /capture >/dev/null
S=$(api_status POST /click '{"ref":"w19"}')
if [ "$S" = "success" ]; then
  pass "Radio click API returned success"
else
  fail "Radio click API returned: $S"
  FAILED=$((FAILED+1))
fi

sleep 0.5
W23=$(capture_ref_text "w23")
if [ "$W23" = "selected=A" ]; then
  pass "Radio output shows 'selected=A'"
else
  fail "Radio output: expected 'selected=A', got '$W23'"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 8: Long Press ---
info "Test 8: Long press (w28 - Press and hold here)"
# Long press area 可能不在可视区域（CSS y=1064 > 视口高度约 840），需要先滚动
adb shell input swipe 540 1500 540 600 300
sleep 1
api_status GET /capture >/dev/null
S=$(api_status POST /long_press '{"ref":"w28","duration":600}')
if [ "$S" = "success" ]; then
  pass "Long press API returned success"
else
  fail "Long press API returned: $S"
  FAILED=$((FAILED+1))
fi

sleep 1
W29=$(capture_ref_text "w29")
if [ "$W29" = "Long pressed!" ]; then
  pass "Long press output shows 'Long pressed!'"
else
  fail "Long press output: expected 'Long pressed!', got '$W29'"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 9: Swipe ---
info "Test 9: Swipe up (w31 - scroll container)"
api_status GET /capture >/dev/null
S=$(api_status POST /swipe '{"direction":"up","ref":"w31"}')
if [ "$S" = "success" ]; then
  pass "Swipe API returned success"
else
  fail "Swipe API returned: $S"
  FAILED=$((FAILED+1))
fi
echo ""

# --- Test 10: Press Key ---
info "Test 10: Press key (enter)"
S=$(api_status POST /press_key '{"key":"enter"}')
if [ "$S" = "success" ]; then
  pass "Press key API returned success"
else
  fail "Press key API returned: $S"
  FAILED=$((FAILED+1))
fi
echo ""

# ── 结果汇总 ──────────────────────────────────────────
echo "═══════════════════════════════════════════════════"
if [ $FAILED -eq 0 ]; then
  printf "${GREEN}All tests passed!${NC}\n"
else
  printf "${RED}%d test(s) failed${NC}\n" "$FAILED"
fi
echo "═══════════════════════════════════════════════════"

exit $FAILED
