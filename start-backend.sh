#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

export TPS_DB_USERNAME="${TPS_DB_USERNAME:-root}"
export TPS_DB_PASSWORD="${TPS_DB_PASSWORD:-root}"
export TPS_DB_URL="${TPS_DB_URL:-jdbc:mysql://localhost:3306/tps?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}"
export TPS_UPLOAD_DIR="${TPS_UPLOAD_DIR:-$ROOT_DIR/img}"
export SERVER_PORT="${SERVER_PORT:-8080}"
LAN_IP="${TPS_LAN_IP:-$(ip route get 1.1.1.1 2>/dev/null | awk '{for (i=1; i<=NF; i++) if ($i == "src") {print $(i+1); exit}}')}"
LAN_IP="${LAN_IP:-$(ip -4 addr show wlan0 2>/dev/null | awk '/inet / {sub(/\/.*/, "", $2); print $2; exit}')}"
LAN_IP="${LAN_IP:-127.0.0.1}"
export TPS_PUBLIC_BASE_URL="${TPS_PUBLIC_BASE_URL:-http://$LAN_IP:$SERVER_PORT}"

mkdir -p "$TPS_UPLOAD_DIR"

if ss -lnt "sport = :$SERVER_PORT" 2>/dev/null | awk 'NR > 1 {found=1} END {exit found ? 0 : 1}'; then
  echo "Port $SERVER_PORT is already in use."
  echo "If this is TPS backend, it is already running:"
  echo "  curl -i http://127.0.0.1:$SERVER_PORT/api/files/ping"
  echo
  echo "To restart it:"
  echo "  pgrep -af 'spring-boot:run|TpsApplication|tps-backend'"
  echo "  kill <PID>"
  echo "  ./start-backend.sh"
  exit 1
fi

echo "Starting TPS backend"
echo "Backend dir: $BACKEND_DIR"
echo "Database: $TPS_DB_URL"
echo "Database user: $TPS_DB_USERNAME"
echo "Upload dir: $TPS_UPLOAD_DIR"
echo "Public base URL: $TPS_PUBLIC_BASE_URL"
echo
echo "Local test: curl -X POST 'http://127.0.0.1:$SERVER_PORT/api/auth/code?phone=13800138000'"
echo "LAN Swagger: $TPS_PUBLIC_BASE_URL/swagger-ui.html"
echo

cd "$BACKEND_DIR"
exec mvn spring-boot:run
