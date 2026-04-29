# TPS Android 真机联调指南

**版本：** v1.0  
**日期：** 2026-04-28  
**目标：** 确保 TPS 后端可以被安卓手机 APP 稳定访问

---

## 1. 核心原则

Android 真机与后端联调时，最常见问题不是业务代码，而是网络地址、HTTP 明文、图片 URL、WebSocket 鉴权和 token 过期。

因此后端必须先保证：

- 手机能访问 REST API。
- 手机能加载图片。
- 手机能连接 WebSocket。
- 所有登录后接口能正确携带并校验 token。

---

## 2. 地址配置

### 2.1 不同环境地址

| 场景 | Android 端 BASE_URL |
|------|---------------------|
| 模拟器访问电脑后端 | `http://10.0.2.2:8080/` |
| 真机访问电脑后端 | `http://<电脑局域网IP>:8080/` |
| 生产环境 | `https://api.example.com/` |

真机不能使用：

```text
http://localhost:8080/
http://127.0.0.1:8080/
```

这些地址在手机上指向手机自己，不是开发电脑。

### 2.2 真机联调步骤

1. 电脑和手机连接同一个 Wi-Fi。
2. 查看电脑局域网 IP，例如 `192.168.1.20`。
3. 启动后端监听 `0.0.0.0:8080` 或默认 Spring Boot 端口。
4. 手机浏览器访问 `http://192.168.1.20:8080/swagger-ui.html`。
5. APP 配置 `BASE_URL=http://192.168.1.20:8080/`。

---

## 3. Android HTTP 明文限制

开发阶段如果使用 HTTP，Android 需要允许 cleartext。

建议开发阶段：

- debug 包允许 HTTP。
- release 包只允许 HTTPS。

生产阶段：

- 使用 HTTPS。
- WebSocket 使用 `wss://`。
- 不允许明文 HTTP。

---

## 4. 图片访问要求

后端当前图片上传返回 `/img/xxx.jpg` 这类相对路径。Android 真机更适合使用完整 URL：

```text
http://192.168.1.20:8080/img/xxx.jpg
```

后端应在商品列表、商品详情、用户头像中统一返回完整 URL。

移动端图片字段要求：

| 字段 | 要求 |
|------|------|
| `avatarUrl` | 完整 URL 或 `null` |
| `sellerAvatar` | 完整 URL 或 `null` |
| `imageUrls[]` | 完整 URL 数组 |
| `productCover` | 完整 URL 或 `null` |

---

## 5. Token 处理

APP 登录成功后保存：

- `accessToken`
- `refreshToken`
- `userId`
- `role`

每个受保护接口携带：

```http
Authorization: Bearer <accessToken>
```

推荐 Android 处理逻辑：

1. 普通接口返回 `401`。
2. APP 调用 `/api/auth/refresh`。
3. 刷新成功后重试原请求。
4. 刷新失败则清空本地 token 并跳转登录页。

---

## 6. WebSocket 联调

### 6.1 地址

| 场景 | WebSocket 地址 |
|------|----------------|
| 模拟器 | `ws://10.0.2.2:8080/ws` |
| 真机 | `ws://<电脑局域网IP>:8080/ws` |
| 生产 | `wss://api.example.com/ws` |

### 6.2 鉴权

连接时需要携带：

```http
Authorization: Bearer <accessToken>
```

服务端必须从 token 解析用户身份，不允许 Android 端自己传 `senderId` 作为可信身份。

### 6.3 订阅与发送

订阅：

```text
/topic/conversation.{conversationId}
```

发送：

```text
/app/chat.send
```

消息：

```json
{
  "conversationId": 1,
  "content": "你好",
  "type": "TEXT"
}
```

---

## 7. 移动端页面与接口依赖

| 页面 | 依赖接口 | 后端要求 |
|------|----------|----------|
| 登录页 | `/api/auth/login` | 返回 token、role、用户摘要 |
| 注册页 | `/api/auth/code`、`/api/auth/register` | 验证码和注册流程稳定 |
| 首页 | `/api/products` | 分页、图片完整 URL |
| 搜索页 | `/api/products/search` | 支持关键词、分类、价格、成色 |
| 商品详情 | `/api/products/{id}` | 返回卖家、图片、收藏状态 |
| 发布商品 | `/api/files/upload`、`/api/products` | 图片上传稳定 |
| 收藏页 | `/api/favorites` | 返回商品摘要 |
| 聊天列表 | `/api/messages/conversations` | 返回对方信息、未读数 |
| 聊天页 | `/api/messages/{conversationId}`、WebSocket | 支持分页和实时消息 |
| 订单页 | `/api/orders/my` | 买入 / 卖出订单可区分 |
| 通知页 | `/api/notifications` | 按时间倒序 |
| 管理员页 | `/api/admin/**` | ADMIN 角色可访问 |

---

## 8. 联调顺序

推荐按以下顺序，不要一开始就全量联调：

1. 健康检查：手机浏览器访问 Swagger。
2. 认证：注册、登录、保存 token。
3. 商品：列表、详情、图片加载。
4. 发布：上传图片、发布商品、刷新首页。
5. 收藏：收藏、取消收藏、收藏列表。
6. 订单：创建、支付、发货、确认收货。
7. 消息：创建会话、历史消息、WebSocket 收发。
8. 管理员：role=ADMIN 登录后访问管理接口。

---

## 9. Gemini UI 开发约束

在后端接口未冻结前，Gemini 应先使用 Mock 数据完成 UI：

- 不要把临时字段写死到 ViewModel。
- DTO 字段以 `api-contract.md` 为准。
- 图片使用完整 URL 字段。
- 所有页面都要有 loading、empty、error 状态。
- 登录态、token 过期、无权限要有统一处理。

---

## 10. 真机验收清单

- [ ] 手机可访问 Swagger。
- [ ] 手机 APP 可登录。
- [ ] 首页商品列表可加载。
- [ ] 商品图片可显示。
- [ ] 可上传图片并发布商品。
- [ ] 可收藏和取消收藏。
- [ ] 买家可创建订单。
- [ ] 买家可模拟支付。
- [ ] 卖家可发货。
- [ ] 买家可确认收货。
- [ ] 买卖双方可实时聊天。
- [ ] 管理员可进入管理页面。
- [ ] token 过期后 APP 行为正确。
