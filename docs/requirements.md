# 二手交易平台（TPS）需求文档

**版本：** v1.0
**日期：** 2026-03-18
**平台：** Android 移动端 + 后端服务

---

## 1. 项目概述

TPS（Trade Platform System）是一款面向 Android 移动端的二手商品交易平台，支持用户发布、浏览、搜索二手商品，并完成在线沟通与交易。

---

## 2. 目标用户

- 有二手商品出售需求的个人用户
- 有购买二手商品需求的个人用户
- 平台管理员（通过同一 Android 客户端登录，角色不同则展示不同界面）

---

## 3. 功能需求

### 3.1 用户模块

| 功能 | 描述 |
|------|------|
| 注册 | 手机号 + 验证码注册，填写昵称、头像 |
| 登录 | 手机号 + 密码 / 验证码登录 |
| 忘记密码 | 手机号验证码重置密码 |
| 个人主页 | 展示用户昵称、头像、发布商品数、信用评分 |
| 编辑资料 | 修改昵称、头像、简介、所在地 |
| 实名认证 | 上传身份证（可选，提升信用度） |
| 注销账号 | 用户主动申请注销 |

### 3.2 商品模块

| 功能 | 描述 |
|------|------|
| 发布商品 | 填写标题、描述、价格、分类、成色、图片（最多9张）、所在地 |
| 编辑商品 | 修改已发布商品信息 |
| 下架商品 | 将商品标记为已售出或手动下架 |
| 商品详情 | 展示商品图片、描述、卖家信息、价格、发布时间 |
| 商品列表 | 首页瀑布流展示，支持下拉刷新、上拉加载更多 |
| 商品分类 | 数码、服装、书籍、家居、运动、其他等分类 |
| 商品搜索 | 关键词搜索，支持筛选（价格区间、分类、成色、所在地） |
| 收藏商品 | 收藏 / 取消收藏，查看收藏列表 |
| 商品举报 | 举报违规商品 |

### 3.3 消息模块

| 功能 | 描述 |
|------|------|
| 即时聊天 | 买卖双方一对一文字聊天 |
| 图片消息 | 聊天中发送图片 |
| 消息通知 | 新消息推送通知 |
| 会话列表 | 展示所有聊天会话，显示最新消息和未读数 |

### 3.4 交易模块

| 功能 | 描述 |
|------|------|
| 议价 | 买家在聊天中发起议价 |
| 订单创建 | 双方确认价格后创建订单 |
| 订单状态 | 待付款 → 已付款 → 已发货 → 已完成 / 已取消 |
| 支付 | 集成支付宝 / 微信支付（模拟支付用于开发阶段） |
| 确认收货 | 买家确认收货，交易完成 |
| 评价 | 交易完成后双方互相评价（1-5星 + 文字） |
| 退款申请 | 买家发起退款，卖家确认或平台介入 |

### 3.5 通知模块

| 功能 | 描述 |
|------|------|
| 系统通知 | 平台公告、账号状态变更 |
| 交易通知 | 订单状态变更推送 |
| 消息通知 | 新聊天消息推送 |

### 3.6 管理员模块（同一 Android 客户端，角色登录区分）

管理员使用与普通用户相同的 Android App，登录时后端返回角色信息（`role: ADMIN`），客户端根据角色跳转至管理员专属界面，普通用户无法访问管理页面。

| 功能 | 描述 |
|------|------|
| 用户管理 | 查看用户列表、封禁 / 解封账号 |
| 商品审核 | 审核举报商品，下架违规商品 |
| 订单管理 | 查看所有订单，介入纠纷处理 |
| 数据统计 | 用户数、商品数、交易额等核心指标概览 |
| 系统公告 | 发布平台公告推送给所有用户 |

---

## 4. 非功能需求

| 类型 | 要求 |
|------|------|
| 性能 | 商品列表首屏加载 < 2s，图片懒加载 |
| 安全 | HTTPS 通信，JWT 鉴权，敏感信息加密存储，RBAC 角色权限控制（USER / ADMIN） |
| 兼容性 | Android 8.0（API 26）及以上 |
| 可用性 | 后端服务可用性 ≥ 99.5% |
| 图片存储 | 使用对象存储（如阿里云 OSS / 本地 MinIO）存储商品图片 |

---

## 5. 技术选型

### 5.1 前端（Android）

| 技术 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 架构 | MVVM + Clean Architecture |
| 网络 | Retrofit2 + OkHttp3 |
| 图片加载 | Coil |
| 本地存储 | Room Database |
| 依赖注入 | Hilt |
| 导航 | Jetpack Navigation |
| 状态管理 | StateFlow + ViewModel |
| 推送通知 | Firebase Cloud Messaging (FCM) |

### 5.2 后端

| 技术 | 选型 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.x |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 即时通讯 | WebSocket (STOMP) |
| 认证 | JWT (Spring Security) |
| 文件存储 | MinIO（本地部署）/ 阿里云 OSS |
| API 文档 | Swagger / OpenAPI 3.0 |
| 构建工具 | Maven |

---

## 6. 系统架构

```
┌─────────────────────────────────┐
│         Android App             │
│  (Jetpack Compose + MVVM)       │
└──────────────┬──────────────────┘
               │ HTTPS / WebSocket
┌──────────────▼──────────────────┐
│         Spring Boot 后端         │
│  ┌──────────┐  ┌──────────────┐ │
│  │ REST API │  │ WebSocket    │ │
│  └────┬─────┘  └──────┬───────┘ │
│       │               │         │
│  ┌────▼───────────────▼──────┐  │
│  │       Service Layer       │  │
│  └────┬──────────────────────┘  │
│       │                         │
│  ┌────▼──────┐  ┌────────────┐  │
│  │  MySQL    │  │   Redis    │  │
│  └───────────┘  └────────────┘  │
│                                 │
│  ┌──────────────────────────┐   │
│  │    MinIO (图片存储)       │   │
│  └──────────────────────────┘   │
└─────────────────────────────────┘
```

---

## 7. 数据库设计（概要）

### 核心表

- **users** - 用户信息（id, phone, password_hash, nickname, avatar_url, credit_score, role[USER/ADMIN], created_at）
- **products** - 商品（id, user_id, title, description, price, category, condition, status, location, created_at）
- **product_images** - 商品图片（id, product_id, image_url, sort_order）
- **orders** - 订单（id, product_id, buyer_id, seller_id, price, status, created_at）
- **messages** - 聊天消息（id, conversation_id, sender_id, content, type, created_at）
- **conversations** - 会话（id, buyer_id, seller_id, product_id, last_message, updated_at）
- **favorites** - 收藏（id, user_id, product_id, created_at）
- **reviews** - 评价（id, order_id, reviewer_id, reviewee_id, score, content, created_at）

---

## 8. API 设计（概要）

### 认证

- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `POST /api/auth/refresh` - 刷新 Token

### 用户

- `GET /api/users/{id}` - 获取用户信息
- `PUT /api/users/me` - 更新个人信息

### 商品

- `GET /api/products` - 商品列表（分页、筛选）
- `POST /api/products` - 发布商品
- `GET /api/products/{id}` - 商品详情
- `PUT /api/products/{id}` - 编辑商品
- `DELETE /api/products/{id}` - 下架商品
- `GET /api/products/search` - 搜索商品

### 订单

- `POST /api/orders` - 创建订单
- `GET /api/orders` - 我的订单列表
- `PUT /api/orders/{id}/status` - 更新订单状态

### 消息

- `GET /api/conversations` - 会话列表
- `GET /api/conversations/{id}/messages` - 消息记录
- `WS /ws/chat` - WebSocket 聊天连接

### 收藏

- `POST /api/favorites/{productId}` - 收藏商品
- `DELETE /api/favorites/{productId}` - 取消收藏
- `GET /api/favorites` - 收藏列表

---

## 9. 页面结构（Android）

```
登录页面
├── 普通用户登录 → 普通用户界面（TabBar）
│   ├── 首页 Tab
│   │   ├── 商品列表（瀑布流）
│   │   └── 搜索入口
│   ├── 分类 Tab
│   │   └── 分类商品列表
│   ├── 发布 Tab（+）
│   │   └── 发布商品页面
│   ├── 消息 Tab
│   │   └── 会话列表 → 聊天页面
│   └── 我的 Tab
│       ├── 个人主页
│       ├── 我发布的
│       ├── 我的订单
│       ├── 我的收藏
│       └── 设置
└── 管理员登录 → 管理员界面（TabBar）
    ├── 数据统计 Tab
    ├── 用户管理 Tab
    ├── 商品审核 Tab
    ├── 订单管理 Tab
    └── 系统公告 Tab
```

---

## 10. 开发阶段规划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | 后端基础框架 + 用户注册登录 + 商品 CRUD | 高 |
| Phase 2 | Android 基础页面 + 商品列表/详情 + 发布 | 高 |
| Phase 3 | 即时聊天 (WebSocket) + 消息通知 | 高 |
| Phase 4 | 订单流程 + 模拟支付 + 评价 | 中 |
| Phase 5 | 搜索优化 + 收藏 + 举报 | 中 |
| Phase 6 | 推送通知 (FCM) + 性能优化 | 低 |
| Phase 7 | 管理后台（可选） | 低 |

---

## 11. 待确认事项

1. 是否需要真实支付集成（支付宝/微信），或仅做模拟支付？
2. 图片存储方案：本地 MinIO 还是云 OSS？
3. 是否需要地理位置功能（附近的商品）？
4. 短信验证码服务商选择（阿里云 SMS / 腾讯云 SMS）？
5. 管理后台是否在 v1.0 范围内？
6. 后端部署环境（云服务器型号、操作系统）？
