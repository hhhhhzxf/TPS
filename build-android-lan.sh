#!/usr/bin/env bash
# 脚本说明：Android 局域网构建脚本，负责注入局域网与 USB 兜底地址后生成调试 APK。

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAN_IP="${1:-10.120.190.96}"

API_BASE_URL="http://${LAN_IP}:8080/"
WS_URL="ws://${LAN_IP}:8080/ws/websocket"
USB_API_BASE_URL="http://127.0.0.1:8080/"
USB_WS_URL="ws://127.0.0.1:8080/ws/websocket"
EMU_API_BASE_URL="http://10.0.2.2:8080/"
EMU_WS_URL="ws://10.0.2.2:8080/ws/websocket"

# 同一个 APK 同时写入 USB、本机模拟器和当前局域网地址，真机换联调方式时不必重新改代码。
API_FALLBACKS="${USB_API_BASE_URL},${EMU_API_BASE_URL},${API_BASE_URL}"
WS_FALLBACKS="${USB_WS_URL},${EMU_WS_URL},${WS_URL}"

echo "Building Android debug APK for LAN backend"
echo "API: $API_BASE_URL"
echo "WebSocket: $WS_URL"
echo "Fallback APIs: $API_FALLBACKS"
echo "Fallback WebSockets: $WS_FALLBACKS"
echo

cd "$ROOT_DIR"
./gradlew :app:clean :app:assembleDebug \
  -PTPS_API_BASE_URL="$API_BASE_URL" \
  -PTPS_API_FALLBACK_BASE_URLS="$API_FALLBACKS" \
  -PTPS_WS_URL="$WS_URL" \
  -PTPS_WS_FALLBACK_URLS="$WS_FALLBACKS"

echo
echo "APK ready:"
echo "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
