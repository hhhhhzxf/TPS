#!/usr/bin/env bash
# 脚本说明：后端启动脚本，负责准备环境变量、探测局域网地址并直接运行 Spring Boot 服务。

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

export TPS_DB_USERNAME="${TPS_DB_USERNAME:-root}"
export TPS_DB_PASSWORD="${TPS_DB_PASSWORD:-root}"
export TPS_DB_URL="${TPS_DB_URL:-jdbc:mysql://localhost:3306/tps?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}"
export TPS_UPLOAD_DIR="${TPS_UPLOAD_DIR:-$ROOT_DIR/img}"
export SERVER_PORT="${SERVER_PORT:-8080}"

# 优先兜到常见的 JDK 17 安装位置，避免 Gentoo 上只装了二进制包但未显式导出 JAVA_HOME 时启动失败。
if [ -n "${JAVA_HOME:-}" ] && printf '%s' "$JAVA_HOME" | grep -Eq 'openjdk-bin-17|/17($|/)'; then
  :
elif [ -x /usr/lib/jvm/openjdk-bin-17/bin/java ]; then
  export JAVA_HOME="/usr/lib/jvm/openjdk-bin-17"
elif [ -x /opt/openjdk-bin-17/bin/java ]; then
  export JAVA_HOME="/opt/openjdk-bin-17"
fi

# Maven 本地仓库优先放进当前用户目录；若环境受限，再退回到 /tmp，避免权限问题挡住启动。
if [ -z "${MAVEN_REPO_LOCAL:-}" ]; then
  if mkdir -p "$HOME/.m2/repository" 2>/dev/null; then
    MAVEN_REPO_LOCAL="$HOME/.m2/repository"
  else
    MAVEN_REPO_LOCAL="/tmp/tps-m2/repository"
  fi
fi
mkdir -p "$MAVEN_REPO_LOCAL"

if [ -x "${MAVEN_BIN:-}" ]; then
  MAVEN_CMD="$MAVEN_BIN"
elif [ -x /usr/bin/mvn ]; then
  MAVEN_CMD="/usr/bin/mvn"
else
  MAVEN_CMD="$(command -v mvn 2>/dev/null || true)"
fi

if [ -z "$MAVEN_CMD" ] || [ ! -x "$MAVEN_CMD" ]; then
  echo "Maven executable not found. Install it first, for example:"
  echo "  sudo emerge --ask dev-java/maven-bin"
  exit 1
fi

# 先从系统路由推断当前出网地址，再退回到 wlan0；这样 USB 热点和普通 Wi-Fi 都能尽量自动识别。
LAN_IP="${TPS_LAN_IP:-$(ip route get 1.1.1.1 2>/dev/null | awk '{for (i=1; i<=NF; i++) if ($i == "src") {print $(i+1); exit}}')}"
LAN_IP="${LAN_IP:-$(ip -4 addr show wlan0 2>/dev/null | awk '/inet / {sub(/\/.*/, "", $2); print $2; exit}')}"
LAN_IP="${LAN_IP:-127.0.0.1}"
export TPS_PUBLIC_BASE_URL="${TPS_PUBLIC_BASE_URL:-http://$LAN_IP:$SERVER_PORT}"

mkdir -p "$TPS_UPLOAD_DIR"

# 端口占用时直接拦截，避免把“旧后端没关”和“新后端没起来”混在一起排查。
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
echo "Java home: ${JAVA_HOME:-system-default}"
echo "Maven: $MAVEN_CMD"
echo "Maven repo: $MAVEN_REPO_LOCAL"
echo
echo "Local test: curl -X POST 'http://127.0.0.1:$SERVER_PORT/api/auth/code?phone=13800138000'"
echo "LAN Swagger: $TPS_PUBLIC_BASE_URL/swagger-ui.html"
echo

cd "$BACKEND_DIR"
exec "$MAVEN_CMD" -Dmaven.repo.local="$MAVEN_REPO_LOCAL" spring-boot:run
