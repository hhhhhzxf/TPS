# MY校园淘二手（App 化移动端）

这是基于你的需求继续完善后的版本：从普通 H5 原型升级为可安装的 PWA App，并新增本地后端 API（可持久化到 `db.json`）。

## 已实现能力

- 学号实名登录（前端 + 后端接口）
- 首页浏览、搜索、分类筛选、排序
- 首页商品卡片增强（发布时间、浏览量、信用参考）
- 商品发布（图片链接 + 本地图片预览 + 表单校验提示）
- 收藏商品
- 发起交易意向
- 交易页状态筛选（全部/待确认/待交易/已完成/已拒绝）
- 交易页会话化展示（按商品分组交易意向）
- 交易意向时间线（发起/确认/完成）
- 站内消息关键词搜索（发送人/商品/消息内容）
- 消息未读管理（未读计数、单条/全部已读）
- 卖家同意/拒绝，买家确认完成
- 交易后买家评价卖家，信用分自动更新
- 个人中心：信用分、累计成交、评分、退出登录
- 个人中心：我的收藏区（可详情、取消收藏、直接发起交易）
- 举报商品与管理员审核处理
- 商品详情增强（收藏切换、同类推荐）
- 管理员可下架违规商品（状态变更为“已下架”）
- 管理员用户管理（封禁/解封普通用户）
- 管理员商品治理（状态筛选、关键词巡检、批量下架/恢复）
- PWA 安装（支持“安装App”按钮）
- Service Worker 离线缓存（静态资源）
- 后端 JSON 持久化存储（`db.json`）

## 目录结构

```text
.
├── index.html
├── styles.css
├── app.js
├── manifest.webmanifest
├── sw.js
├── icon.svg
├── server.js
├── db.json
├── capacitor.config.json
├── mobile-web/               # 由脚本生成，供 Capacitor 打包
├── scripts/
│   ├── api-smoke-test.mjs
│   └── prepare-capacitor-web.mjs
└── README.md
```

## 推荐启动方式

### 方式一：前后端一体（最简单）

```bash
cd /home/source/My_github/TPS/exchange
npm run start:api
```

打开：`http://localhost:3000`

### 方式二：前端 9876 + 后端 3000（推荐联调）

先起后端：

```bash
cd /home/source/My_github/TPS/exchange
npm run start:api
```

再起前端：

```bash
cd /home/source/My_github/TPS/exchange
npm run start:web
```

打开：`http://127.0.0.1:9876`

说明：

- `start:web` 会把前端固定跑在 `9876`
- 前端接口默认指向 `http://127.0.0.1:3000`
- 这时登录、发布、交易、举报、管理员操作都会走真实本地接口，不是纯预览模式

### 方式三：仅预览前端样式

```bash
cd /home/source/My_github/TPS/exchange
npm run preview
```

打开：`http://127.0.0.1:9876`

说明：

- `preview` 只用于看界面，不依赖后端
- 它会关闭 service worker 并禁用缓存，避免旧脚本残留

### 重置演示数据

如果联调时写入了很多测试数据，可以执行：

```bash
cd /home/source/My_github/TPS/exchange
npm run reset:db
```

它会把 `db.json` 还原成干净的演示初始数据。

## 快速测试

```bash
cd /home/source/My_github/TPS/exchange
npm run test:smoke
```

该脚本会自动启动服务并验证登录、鉴权、管理员权限、封禁与审计日志链路。

## 打包 Android App（Capacitor）

先安装依赖（首次一次性）：

```bash
cd /home/hzxf/project/exchange
npm i -D @capacitor/cli
npm i @capacitor/core @capacitor/android
```

首次创建 Android 工程：

```bash
npx cap add android
```

日常同步前端资源并打开 Android Studio：

```bash
npm run mobile:android
```

只同步不打开 IDE：

```bash
npm run mobile:sync
```

说明：

- `npm run build:mobile-web` 会生成 `mobile-web/`，把 `index.html/app.js/styles.css/sw.js/manifest/icon` 拷贝给原生壳使用。
- 调试 Web 逻辑仍使用 `node server.js` + 浏览器；打包原生 App 时使用 Capacitor 工程。

## API

- 鉴权：除 `GET /api/health` 与 `POST /api/auth/login` 外，其余接口都需要请求头  
  `Authorization: Bearer <token>`
- Token 为后端签发的 `JWT (HS256)`，登录成功后返回。
- `GET /api/health` 健康检查
- `GET /api/state` 获取全量状态
- `POST /api/auth/login` 学号登录（返回 `token` + `user`）
- `POST /api/goods` 发布商品
- `PATCH /api/goods/:id/status` 更新商品状态（在售/已售）
- `PATCH /api/goods/status-batch` 批量更新商品状态
- `PATCH /api/goods/:id/views` 增加浏览量
- `POST /api/intents` 创建交易意向（可附带私信）
- `PATCH /api/intents/:id` 更新交易意向状态
- `POST /api/ratings` 新增评分并更新信用分
- `POST /api/reports` 提交举报
- `GET /api/reports?status=pending` 查询举报
- `PATCH /api/reports/:id` 审核举报（可执行下架动作）
- `GET /api/users` 获取用户列表
- `PATCH /api/users/:id/status` 封禁/解封用户（`active`/`blocked`）
- `GET /api/audit-logs` 获取管理员操作审计日志（仅管理员）
- `PUT /api/users/:id/favorites` 更新收藏列表
- `PUT /api/state` 兼容旧版全量保存（建议仅用于迁移）

## 如何安装成 App

1. 用 Chrome/Edge 打开项目地址。
2. 点击顶部 `安装App` 按钮。
3. 安装后可从桌面或应用列表独立打开。

## 说明

- 当前是轻量后端方案，适合开发和演示。
- 如果你只用 `python -m http.server` 启动，页面也能运行，但会自动切到离线模式（数据写浏览器本地）。
- 管理员演示账号：学号 `99990000`（任意昵称/邮箱可登录）。
- 被封禁用户将无法发布商品、发起交易、提交举报。

## 下一步建议

1. 接 MySQL + JWT，支持多用户并发和权限控制。
2. 增加管理员后台（违规审核、举报处理、分类管理）。
3. 增加图片上传（对象存储）而不是手填 URL。
4. 改 UniApp/Taro，输出原生 App + 小程序。
