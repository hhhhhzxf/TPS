#!/usr/bin/env bash
# 脚本说明：测试数据脚本，负责批量创建账号、商品与典型交易状态，便于真机联调。

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

post_json() {
  local path="$1"
  local payload="$2"
  curl -sS -X POST "$BASE_URL$path" \
    -H "Content-Type: application/json" \
    -d "$payload"
}

post_auth_json() {
  local path="$1"
  local token="$2"
  local payload="$3"
  curl -sS -X POST "$BASE_URL$path" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -d "$payload"
}

post_auth() {
  local path="$1"
  local token="$2"
  curl -sS -X POST "$BASE_URL$path" \
    -H "Authorization: Bearer $token"
}

patch_auth() {
  local path="$1"
  local token="$2"
  curl -sS -X PATCH "$BASE_URL$path" \
    -H "Authorization: Bearer $token"
}

extract_token() {
  sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

extract_id() {
  sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p' | head -n 1
}

ensure_code() {
  local phone="$1"
  curl -sS -X POST "$BASE_URL/api/auth/code?phone=$phone" >/dev/null
}

register_user() {
  local phone="$1"
  local password="$2"
  local student_id="$3"
  local nickname="$4"
  post_json "/api/auth/register" \
    "{\"phone\":\"$phone\",\"password\":\"$password\",\"code\":\"1234\",\"studentId\":\"$student_id\",\"nickname\":\"$nickname\"}" || true
}

login_user() {
  local phone="$1"
  local password="$2"
  post_json "/api/auth/login" \
    "{\"phone\":\"$phone\",\"password\":\"$password\"}"
}

create_product() {
  local token="$1"
  local payload="$2"
  post_auth_json "/api/products" "$token" "$payload"
}

# 注册前先触发验证码接口；开发环境验证码固定为 1234，因此这里只需要保证服务端状态准备好。
ensure_code "13900000001"
ensure_code "13900000002"
ensure_code "13900000003"

register_user "13900000001" "pass123" "20260001" "测试卖家A" >/tmp/tps_reg_1.json
register_user "13900000002" "pass123" "20260002" "测试卖家B" >/tmp/tps_reg_2.json
register_user "13900000003" "pass123" "20260003" "测试买家C" >/tmp/tps_reg_3.json

login_user "13900000001" "pass123" >/tmp/tps_login_1.json
login_user "13900000002" "pass123" >/tmp/tps_login_2.json
login_user "13900000003" "pass123" >/tmp/tps_login_3.json

TOKEN_1="$(extract_token </tmp/tps_login_1.json)"
TOKEN_2="$(extract_token </tmp/tps_login_2.json)"
TOKEN_3="$(extract_token </tmp/tps_login_3.json)"

# 这里故意使用公网图片地址，便于真机直接覆盖“远程图片加载、列表缩略图、详情大图”三条链路。
IMG_LAPTOP="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=1200&q=80"
IMG_BOOKS="https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=1200&q=80"
IMG_BICYCLE="https://images.unsplash.com/photo-1541625602330-2277a4c46182?auto=format&fit=crop&w=1200&q=80"
IMG_SHOES="https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1200&q=80"
IMG_BOX="https://images.unsplash.com/photo-1585386959984-a4155224a1ad?auto=format&fit=crop&w=1200&q=80"

create_product "$TOKEN_1" \
  "{\"title\":\"九成新戴尔笔记本\",\"description\":\"课程设计、文档办公和线上面试都够用，电池正常，带原装充电器。适合测试商品详情、聊天、收藏、下单流程。\",\"price\":2199.00,\"category\":\"数码\",\"condition\":\"LIKE_NEW\",\"location\":\"一食堂门口\",\"imageUrls\":[\"$IMG_LAPTOP\"]}" >/tmp/tps_product_1.json

create_product "$TOKEN_1" \
  "{\"title\":\"数据结构+操作系统教材打包出\",\"description\":\"两本一起出，带少量笔记，适合测试低价商品、搜索和浏览历史。\",\"price\":48.50,\"category\":\"教材\",\"condition\":\"GOOD\",\"location\":\"图书馆东门\",\"imageUrls\":[\"$IMG_BOOKS\"]}" >/tmp/tps_product_2.json

create_product "$TOKEN_2" \
  "{\"title\":\"捷安特自行车\",\"description\":\"可正常骑行，适合宿舍到教学楼通勤，刹车正常。可用于测试举报与商品状态流转。\",\"price\":399.00,\"category\":\"运动\",\"condition\":\"FAIR\",\"location\":\"南门快递站\",\"imageUrls\":[\"$IMG_BICYCLE\"]}" >/tmp/tps_product_3.json

create_product "$TOKEN_2" \
  "{\"title\":\"42码跑步鞋\",\"description\":\"穿过几次，鞋底磨损轻，尺码不合适所以转出。用于测试下架商品。\",\"price\":128.00,\"category\":\"服饰\",\"condition\":\"LIKE_NEW\",\"location\":\"体育馆\",\"imageUrls\":[\"$IMG_SHOES\"]}" >/tmp/tps_product_4.json

create_product "$TOKEN_2" \
  "{\"title\":\"宿舍收纳箱\",\"description\":\"容量大，适合放书和衣物，成色新。用于测试已售商品展示。\",\"price\":35.00,\"category\":\"生活\",\"condition\":\"NEW\",\"location\":\"12号宿舍楼\",\"imageUrls\":[\"$IMG_BOX\"]}" >/tmp/tps_product_5.json

PRODUCT_1_ID="$(extract_id </tmp/tps_product_1.json)"
PRODUCT_2_ID="$(extract_id </tmp/tps_product_2.json)"
PRODUCT_3_ID="$(extract_id </tmp/tps_product_3.json)"
PRODUCT_4_ID="$(extract_id </tmp/tps_product_4.json)"
PRODUCT_5_ID="$(extract_id </tmp/tps_product_5.json)"

post_auth "/api/products/$PRODUCT_1_ID/bump" "$TOKEN_1" >/tmp/tps_bump_1.json
patch_auth "/api/products/$PRODUCT_4_ID/status?status=OFF" "$TOKEN_2" >/tmp/tps_off_4.json
patch_auth "/api/products/$PRODUCT_5_ID/status?status=SOLD" "$TOKEN_2" >/tmp/tps_sold_5.json
post_auth "/api/favorites/$PRODUCT_1_ID/toggle" "$TOKEN_3" >/tmp/tps_favorite_1.json
post_auth "/api/products/$PRODUCT_3_ID/report?reason=测试举报链路" "$TOKEN_3" >/tmp/tps_report_3.json

printf 'TEST_USERS\n'
printf '卖家A: 13900000001 / pass123\n'
printf '卖家B: 13900000002 / pass123\n'
printf '买家C: 13900000003 / pass123\n'
printf '\nPRODUCT_IDS\n'
printf '笔记本=%s 教材=%s 自行车=%s 跑步鞋=%s 收纳箱=%s\n' \
  "$PRODUCT_1_ID" "$PRODUCT_2_ID" "$PRODUCT_3_ID" "$PRODUCT_4_ID" "$PRODUCT_5_ID"
printf '\nSTATE_HINTS\n'
printf '笔记本: 在售 + 已擦亮 + 被买家C收藏\n'
printf '教材: 在售\n'
printf '自行车: 在售 + 已被买家C举报\n'
printf '跑步鞋: 已下架\n'
printf '收纳箱: 已售出\n'
