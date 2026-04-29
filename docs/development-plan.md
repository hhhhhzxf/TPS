# TPS 二手交易平台 — 开发计划文档

**版本：** v1.1
**日期：** 2026-04-28

> 当前执行策略：后端优先完善，Android APP 界面可并行使用 Mock 数据开发；待后端接口契约稳定后进入真机联调。

---

## 0. 文档索引

| 文档 | 说明 |
|------|------|
| `README.md` | 文档中心和协作规则 |
| `requirements.md` | 产品需求与功能范围 |
| `development-plan.md` | 总体开发计划 |
| `backend-completion-plan.md` | 后端完成计划、缺口和验收标准 |
| `api-contract.md` | REST / WebSocket 接口契约 |
| `android-integration-guide.md` | Android 真机联调说明 |
| `production-readiness.md` | 生产上线检查清单 |

---

## 1. 技术选型（已确认）

| 项目 | 选型 |
|------|------|
| 后端语言 | Java 17 |
| 后端框架 | Spring Boot 3.x + Maven |
| 数据库 | MySQL 8.0 + Redis |
| 即时通讯 | WebSocket (STOMP) |
| 认证 | JWT + Spring Security（RBAC：USER / ADMIN）|
| 图片存储 | 本地文件系统（`img/` 目录，HTTP 静态资源暴露）|
| 支付 | 模拟支付（接口直接返回成功）|
| 短信验证码 | 固定 1234（开发用）|
| 地理位置 | 不做 |
| Android 语言 | Kotlin |
| Android UI | Jetpack Compose |
| 架构模式 | MVVM + Clean Architecture |
| 网络库 | Retrofit2 + OkHttp3 |
| 图片加载 | Coil |
| 本地存储 | Room Database |
| 依赖注入 | Hilt |
| 推送通知 | FCM（Phase 7）|

---

## 2. 项目目录结构

```
TPS/
├── docs/                         # 文档
│   ├── README.md                 # 文档中心
│   ├── requirements.md           # 需求文档
│   ├── development-plan.md       # 总体开发计划
│   ├── backend-completion-plan.md# 后端完成计划
│   ├── api-contract.md           # 接口契约
│   ├── android-integration-guide.md # Android 真机联调
│   └── production-readiness.md   # 生产就绪清单
├── backend/                      # Spring Boot 后端
│   ├── src/main/java/com/tps/
│   │   ├── config/               # Security、WebSocket、静态资源配置
│   │   ├── controller/           # REST API 控制器
│   │   ├── service/              # 业务逻辑层
│   │   ├── repository/           # JPA Repository
│   │   ├── entity/               # 数据库实体
│   │   ├── dto/                  # 请求/响应 DTO
│   │   ├── security/             # JWT 工具类、过滤器
│   │   └── websocket/            # WebSocket 消息处理
│   ├── src/main/resources/
│   │   ├── application.yml       # 配置文件
│   │   └── sql/init.sql          # 数据库初始化脚本
│   └── pom.xml
├── app/                          # Android 客户端
│   ├── src/main/java/com/tps/
│   │   ├── data/
│   │   │   ├── api/              # Retrofit 接口定义
│   │   │   ├── repository/       # 数据仓库实现
│   │   │   └── local/            # Room 数据库
│   │   ├── domain/
│   │   │   ├── model/            # 领域模型
│   │   │   └── usecase/          # 业务用例
│   │   ├── ui/
│   │   │   ├── auth/             # 登录、注册页面
│   │   │   ├── home/             # 首页商品列表
│   │   │   ├── product/          # 商品详情、发布
│   │   │   ├── chat/             # 聊天页面
│   │   │   ├── order/            # 订单页面
│   │   │   ├── profile/          # 个人中心
│   │   │   └── admin/            # 管理员专属页面
│   │   └── di/                   # Hilt 依赖注入模块
│   └── build.gradle.kts
└── img/                          # 图片存储目录（后端静态资源）
```

---

## 3. 数据库设计

```sql
-- 用户表
CREATE TABLE users (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone        VARCHAR(11) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  nickname     VARCHAR(50),
  avatar_url   VARCHAR(255),
  credit_score INT DEFAULT 100,
  role         ENUM('USER', 'ADMIN') DEFAULT 'USER',
  status       ENUM('ACTIVE', 'BANNED') DEFAULT 'ACTIVE',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 商品表
CREATE TABLE products (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(100) NOT NULL,
  description TEXT,
  price       DECIMAL(10,2) NOT NULL,
  category    VARCHAR(30),
  `condition` ENUM('NEW','LIKE_NEW','GOOD','FAIR') DEFAULT 'GOOD',
  status      ENUM('ON_SALE','SOLD','OFF') DEFAULT 'ON_SALE',
  location    VARCHAR(100),
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 商品图片表
CREATE TABLE product_images (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  image_url  VARCHAR(255) NOT NULL,
  sort_order INT DEFAULT 0
);

-- 收藏表
CREATE TABLE favorites (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id)
);

-- 会话表
CREATE TABLE conversations (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  buyer_id       BIGINT NOT NULL,
  seller_id      BIGINT NOT NULL,
  product_id     BIGINT NOT NULL,
  last_message   VARCHAR(255),
  unread_buyer   INT DEFAULT 0,
  unread_seller  INT DEFAULT 0,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 消息表
CREATE TABLE messages (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id       BIGINT NOT NULL,
  content         TEXT NOT NULL,
  type            ENUM('TEXT','IMAGE') DEFAULT 'TEXT',
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE orders (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  buyer_id   BIGINT NOT NULL,
  seller_id  BIGINT NOT NULL,
  price      DECIMAL(10,2) NOT NULL,
  status     ENUM('PENDING','PAID','SHIPPED','DONE','CANCELLED') DEFAULT 'PENDING',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 评价表
CREATE TABLE reviews (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id     BIGINT NOT NULL,
  reviewer_id  BIGINT NOT NULL,
  reviewee_id  BIGINT NOT NULL,
  score        INT NOT NULL,
  content      VARCHAR(500),
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 举报表
CREATE TABLE reports (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  product_id  BIGINT NOT NULL,
  reason      VARCHAR(255),
  status      ENUM('PENDING','HANDLED') DEFAULT 'PENDING',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 通知表
CREATE TABLE notifications (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL,
  type       VARCHAR(30),
  content    VARCHAR(500),
  is_read    TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. API 接口清单

### 认证
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/auth/sms | 发送验证码（固定返回 1234）|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录（返回 JWT + role）|
| POST | /api/auth/refresh | 刷新 Token |

### 用户
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/users/{id} | 获取用户信息 |
| PUT | /api/users/me | 更新个人信息 |
| POST | /api/upload/image | 上传图片（存 img/ 目录）|

### 商品
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/products | 列表（分页 + 筛选）|
| POST | /api/products | 发布商品 |
| GET | /api/products/{id} | 商品详情 |
| PUT | /api/products/{id} | 编辑商品 |
| DELETE | /api/products/{id} | 下架商品 |
| GET | /api/products/search | 搜索商品 |

### 收藏
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/favorites/{productId} | 收藏 |
| DELETE | /api/favorites/{productId} | 取消收藏 |
| GET | /api/favorites | 收藏列表 |

### 会话 & 消息
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/conversations | 会话列表 |
| GET | /api/conversations/{id}/messages | 历史消息 |
| WS | /ws/chat | WebSocket 聊天（STOMP）|

### 订单
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/orders | 创建订单 |
| GET | /api/orders | 我的订单列表 |
| PUT | /api/orders/{id}/pay | 模拟支付 |
| PUT | /api/orders/{id}/ship | 标记发货 |
| PUT | /api/orders/{id}/confirm | 确认收货 |
| POST | /api/orders/{id}/review | 提交评价 |
| POST | /api/orders/{id}/refund | 申请退款 |

### 管理员（需 ADMIN 角色）
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/admin/stats | 平台数据统计 |
| GET | /api/admin/users | 用户列表 |
| PUT | /api/admin/users/{id}/ban | 封禁用户 |
| PUT | /api/admin/users/{id}/unban | 解封用户 |
| GET | /api/admin/products/reported | 被举报商品 |
| PUT | /api/admin/products/{id}/takedown | 强制下架 |
| GET | /api/admin/orders | 所有订单 |
| POST | /api/admin/announcements | 发布公告 |

---

## 5. Android 页面结构

```
登录页 / 注册页
  └── 登录成功 → 根据 role 路由
        ├── role=USER  → 普通用户 MainScreen（BottomNavigation）
        │     ├── 首页 Tab      — 商品瀑布流、搜索
        │     ├── 分类 Tab      — 分类商品列表
        │     ├── 发布 Tab(+)   — 发布商品
        │     ├── 消息 Tab      — 会话列表 → 聊天页
        │     └── 我的 Tab      — 个人主页、我发布的、我的订单、收藏、设置
        └── role=ADMIN → 管理员 AdminScreen（BottomNavigation）
              ├── 统计 Tab      — 数据统计卡片
              ├── 用户 Tab      — 用户列表、封禁操作
              ├── 商品 Tab      — 举报商品审核
              ├── 订单 Tab      — 订单列表、纠纷处理
              └── 公告 Tab      — 发布系统公告
```

---

## 6. 开发阶段计划

| 阶段 | 内容 | 交付物 |
|------|------|--------|
| **Phase 1** | 后端基础框架：项目初始化、建表、JWT 认证、用户模块、商品 CRUD、图片上传 | 可用 REST API（Swagger 可测）|
| **Phase 2** | Android 基础：项目初始化、登录注册、首页商品列表、商品详情、发布商品、个人中心、管理员框架 | App 可浏览/发布商品 |
| **Phase 3** | 即时聊天：WebSocket STOMP、消息持久化、聊天页面、会话列表、未读角标 | 买卖双方实时聊天 |
| **Phase 4** | 订单交易：订单流程、模拟支付、评价、退款 | 完整交易闭环 |
| **Phase 5** | 管理员功能：后端 ADMIN 接口、Android 管理员完整页面 | 管理员可完成所有管理操作 |
| **Phase 6** | 收藏、举报、通知 | 完善用户功能 |
| **Phase 7** | FCM 推送、性能优化、接口限流 | 生产就绪 |

---

## 7. 当前执行状态

- [x] 需求文档完成
- [x] 总体开发计划文档完成
- [x] 后端完成计划文档完成
- [x] API 契约文档骨架完成
- [x] Android 真机联调文档完成
- [x] 生产就绪清单完成
- [~] Phase 1 — 后端基础框架：已有骨架，需按 `backend-completion-plan.md` 加固
- [~] Phase 2 — Android 基础页面：可由 Gemini 使用 Mock 数据并行开发
- [~] Phase 3 — 即时聊天：已有基础实现，需补 WebSocket 鉴权和权限校验
- [~] Phase 4 — 订单交易：已有基础实现，需补并发防护、评价、退款
- [~] Phase 5 — 管理员功能：已有基础实现，需 DTO 化和处理闭环
- [~] Phase 6 — 收藏举报通知：收藏和通知已有，举报用户入口需补齐
- [ ] Phase 7 — 优化推送与生产加固

---

## 8. 当前后端优先执行路线

| 顺序 | 阶段 | 目标 | 依据文档 |
|------|------|------|----------|
| 1 | 认证与权限 | token 续期、权限、封禁状态可靠 | `backend-completion-plan.md` |
| 2 | API 契约 | 固定字段、错误码、分页结构 | `api-contract.md` |
| 3 | 商品与图片 | Android 真机可浏览和发布商品 | `android-integration-guide.md` |
| 4 | 消息与 WebSocket | 真机可安全聊天 | `api-contract.md` |
| 5 | 订单交易 | 防重复购买，状态机可靠 | `backend-completion-plan.md` |
| 6 | 评价举报管理 | 平台业务闭环完整 | `backend-completion-plan.md` |
| 7 | 生产准备 | 配置、安全、部署、测试完成 | `production-readiness.md` |
