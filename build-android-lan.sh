#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAN_IP="${1:-10.120.190.96}"

API_BASE_URL="http://${LAN_IP}:8080/"
WS_URL="ws://${LAN_IP}:8080/ws/websocket"

echo "Building Android debug APK for LAN backend"
echo "API: $API_BASE_URL"
echo "WebSocket: $WS_URL"
echo

cd "$ROOT_DIR"
./gradlew :app:clean :app:assembleDebug \
  -PTPS_API_BASE_URL="$API_BASE_URL" \
  -PTPS_API_FALLBACK_BASE_URLS="$API_BASE_URL" \
  -PTPS_WS_URL="$WS_URL" \
  -PTPS_WS_FALLBACK_URLS="$WS_URL"

echo
echo "APK ready:"
echo "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
