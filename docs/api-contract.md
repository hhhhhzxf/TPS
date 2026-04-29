# TPS API 契约文档

**版本：** v1.0  
**日期：** 2026-04-28  
**目标读者：** 后端、Android APP、Gemini UI 实现方

---

## 1. 基础约定

### 1.1 环境地址

| 环境 | REST BASE_URL | WebSocket |
|------|---------------|-----------|
| 本机开发 | `http://localhost:8080` | `ws://localhost:8080/ws` |
| Android 模拟器 | `http://10.0.2.2:8080` | `ws://10.0.2.2:8080/ws` |
| Android 真机局域网 | `http://<电脑局域网IP>:8080` | `ws://<电脑局域网IP>:8080/ws` |
| 生产 | `https://api.example.com` | `wss://api.example.com/ws` |

> 真机不能使用 `localhost` 访问电脑后端。

### 1.2 认证方式

除公开接口外，请求头必须携带：

```http
Authorization: Bearer <accessToken>
```

公开接口：

- `POST /api/auth/code`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/products/search`
- `GET /img/**`

### 1.3 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页接口统一返回移动端友好的结构，不直接暴露 Spring Page 内部字段：

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

## 2. 错误码约定

| code | 含义 | APP 处理 |
|------|------|----------|
| 200 | 成功 | 正常解析 `data` |
| 400 | 参数错误 / 业务规则失败 | Toast / 表单提示 |
| 401 | 未登录 / token 无效 | 跳转登录或刷新 token |
| 403 | 权限不足 | 提示无权限 |
| 404 | 资源不存在 | 展示空状态或返回上一页 |
| 409 | 业务冲突 | 提示状态已变化，例如商品已售出 |
| 429 | 请求过频 | 提示稍后重试 |
| 500 | 服务端错误 | 提示服务器异常 |

后端安全层也返回同样的 JSON 结构，`401` 和 `403` 不返回 HTML 页面。

---

## 3. 认证接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/auth/code?phone=` | 发送验证码 | 已有，开发固定 `1234` |
| POST | `/api/auth/register` | 注册并返回 token | 已有 |
| POST | `/api/auth/login` | 登录并返回 token、role | 已有 |
| POST | `/api/auth/refresh` | 刷新 token | 已有 |
| POST | `/api/auth/logout` | 登出，当前为无状态 JWT 客户端清 token | 已有 |

登录响应必须包含：

```json
{
  "token": "access-token",
  "refreshToken": "refresh-token",
  "userId": 1,
  "nickname": "用户0000",
  "avatarUrl": "http://host/img/a.jpg",
  "role": "USER"
}
```

---

## 4. 用户接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/users/me` | 当前用户资料 | 已有 |
| PUT | `/api/users/me` | 修改资料 | 已有 |
| PUT | `/api/users/me/avatar?avatarUrl=` | 修改头像 | 已有 |
| GET | `/api/users/{id}` | 用户主页 | 已有 |
| POST | `/api/users/me/deactivate` | 注销账号 | 已有 |

用户资料建议返回：

- `id`
- `phone`
- `nickname`
- `avatarUrl`
- `bio`
- `location`
- `creditScore`
- `role`
- `status`
- `productCount`

管理员用户列表也使用该结构，不返回 `passwordHash` 等实体敏感字段。

---

## 5. 商品接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/products` | 商品列表 | 已有 |
| GET | `/api/products/search` | 搜索 / 筛选 | 已有 |
| GET | `/api/products/{id}` | 商品详情 | 已有 |
| POST | `/api/products` | 发布商品 | 已有 |
| PUT | `/api/products/{id}` | 编辑商品 | 已有 |
| PATCH | `/api/products/{id}/status` | 修改状态 | 已有 |
| POST | `/api/products/{id}/bump` | 擦亮商品，每件每天最多 3 次 | 已有 |
| GET | `/api/products/my` | 我的商品 | 已有 |
| POST | `/api/products/{id}/report` | 举报商品 | 已有 |

商品状态：

- `ON_SALE`
- `SOLD`
- `OFF`

商品成色：

- `NEW`
- `LIKE_NEW`
- `GOOD`
- `FAIR`

移动端要求：

- `imageUrls` 必须是完整 URL。
- 详情接口需要返回 `favorited`。
- 列表和详情返回 `viewCount`、`favoriteCount`、`bumpedAt`。
- 列表接口建议返回首图，避免移动端加载过多字段。

---

## 6. 文件接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/files/upload` | 上传单张图片 | 已有，已校验图片类型和文件头 |

请求：

```http
Content-Type: multipart/form-data
file=<image>
```

响应建议：

```json
{
  "url": "http://host/img/uuid.jpg",
  "path": "/img/uuid.jpg"
}
```

后端必须校验：

- 文件大小
- MIME 类型
- 文件扩展名
- 图片文件头

---

## 7. 收藏接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/favorites/{productId}` | 收藏 | 已有 |
| DELETE | `/api/favorites/{productId}` | 取消收藏 | 已有 |
| POST | `/api/favorites/{productId}/toggle` | 收藏切换 | 已有，兼容旧调用 |
| GET | `/api/favorites` | 收藏列表 | 已有 |
| GET | `/api/favorites/{productId}/status` | 查询收藏状态 | 已有 |

建议避免 toggle 作为唯一接口，因为 Android 端在弱网和重试场景下容易状态错乱。

---

## 8. 消息接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/messages/conversation` | 获取或创建会话 | 已有 |
| GET | `/api/messages/conversations` | 会话列表 | 已有 |
| GET | `/api/messages/{conversationId}` | 历史消息 | 已有，已校验会话成员 |
| PUT | `/api/messages/{conversationId}/read` | 标记已读 | 已有 |

会话列表建议返回：

- `id`
- `productId`
- `productTitle`
- `productPrice`
- `productImageUrl`
- `productCover`
- `targetUserId`
- `targetNickname`
- `targetAvatarUrl`
- `targetAvatar`
- `lastMessage`
- `unreadCount`
- `updatedAt`

---

## 9. WebSocket 契约

当前端点：

- Endpoint：`/ws`
- App Prefix：`/app`
- Topic：`/topic/conversation.{conversationId}`
- Send：`/app/chat.send`

消息格式：

```json
{
  "conversationId": 1,
  "content": "你好",
  "type": "TEXT"
}
```

后端必须从认证上下文确定 `senderId`，不允许客户端提交或覆盖 `senderId`。

Android 端连接时需要携带 token：

```http
Authorization: Bearer <accessToken>
```

---

## 10. 订单接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| POST | `/api/orders` | 创建订单 | 已有 |
| GET | `/api/orders/my?role=buyer&status=PENDING` | 我的买入订单，可按状态筛选 | 已有 |
| GET | `/api/orders/my?role=seller&status=PAID` | 我的卖出订单，可按状态筛选 | 已有 |
| GET | `/api/orders/{id}` | 订单详情 | 已有 |
| PUT | `/api/orders/{id}/pay` | 模拟支付 | 已有 |
| PUT | `/api/orders/{id}/ship` | 发货，可传 `trackingNumber` | 已有 |
| PUT | `/api/orders/{id}/confirm` | 确认收货 | 已有 |
| PUT | `/api/orders/{id}/cancel` | 取消订单 | 已有 |
| POST | `/api/orders/{id}/review` | 评价 | 已有 |
| POST | `/api/orders/{id}/refund` | 申请退款 | 已有 |
| PUT | `/api/orders/{id}/refund/approve` | 卖家同意退款 | 已有 |

订单接口统一返回 `OrderResponse`：

- `id`
- `productId`
- `productTitle`
- `productCover`
- `buyerId`
- `buyerNickname`
- `sellerId`
- `sellerNickname`
- `price`
- `status`
- `remark`
- `trackingNumber`
- `createdAt`
- `updatedAt`

订单状态：

- `PENDING`
- `PAID`
- `SHIPPED`
- `DONE`
- `CANCELLED`
- `REFUNDING`
- `REFUNDED`

---

## 11. 通知接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/notifications?type=MESSAGE` | 通知列表，可按类型分组 | 已有 |
| PUT | `/api/notifications/{id}/read` | 标记单条已读 | 已有 |
| PUT | `/api/notifications/read-all` | 全部已读 | 已有 |
| PATCH | `/api/notifications/read-all` | 全部已读，兼容 Android PATCH 调用 | 已有 |

通知类型建议：

- `SYSTEM`
- `MESSAGE`
- `ORDER`
- `REPORT`
- `REFUND`

---

## 12. 管理员接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/admin/stats` | 平台统计 | 已有 |
| GET | `/api/admin/users` | 用户列表 | 已有 |
| PUT | `/api/admin/users/{id}/ban` | 封禁用户 | 已有 |
| PUT | `/api/admin/users/{id}/unban` | 解封用户 | 已有 |
| GET | `/api/admin/products/reported` | 举报商品 | 已有 |
| PUT | `/api/admin/products/{id}/takedown` | 下架商品 | 已有 |
| PUT | `/api/admin/reports/{id}/handle` | 处理举报 | 已有 |
| GET | `/api/admin/orders` | 订单列表 | 已有 |
| GET | `/api/admin/orders/refunding` | 退款中订单 | 已有 |
| PUT | `/api/admin/orders/{id}/refund/approve` | 管理员同意退款 | 已有 |
| POST | `/api/admin/notifications` | 发布公告 | 已有 |

管理员接口必须使用 DTO，不能直接返回包含敏感字段的实体。
