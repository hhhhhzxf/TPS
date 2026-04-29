# TPS 校园二手交易系统

TPS 是一个面向校园场景的二手交易系统，包含 Android 用户端、Spring Boot 后端、管理端接口和本地联调脚本。系统支持商品发布、图片上传、收藏、浏览历史、订单流转、聊天、个人资料、反馈处理和管理员后台能力。

## 功能概览

- 用户认证：手机号/学号登录、注册、JWT 鉴权、刷新令牌。
- 商品交易：商品发布、图片上传、首页浏览、搜索筛选、收藏、浏览历史、我发布的商品管理。
- 订单闭环：立即购买、支付、发货、确认收货、取消订单、退款相关状态。
- 聊天消息：商品会话、REST 消息兜底、WebSocket/STOMP 实时推送。
- 个人中心：头像、昵称、学号、收货地址、订单、收藏、历史、反馈。
- 管理后台：用户管理、商品下架、举报处理、订单查看、反馈回复。

## 技术栈

后端：

- Java 17
- Spring Boot 3.2.3
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.x
- Redis
- WebSocket/STOMP
- SpringDoc OpenAPI

Android：

- Kotlin
- Jetpack Compose
- Hilt
- Retrofit + OkHttp
- Coil
- DataStore
- Navigation Compose
- Gradle Kotlin DSL

Web 静态资源：

- `exchange/` 保留了一套移动 Web 静态资源，Android 构建任务会同步其中部分文件到 APK assets。

## 目录结构

```text
TPS/
├── app/                    # Android 客户端
│   └── src/main/java/com/tps/
├── backend/                # Spring Boot 后端
│   └── src/main/java/com/tps/
├── docs/                   # 需求、接口、计划和交付文档
├── exchange/               # Web 静态资源
├── gradle/                 # Gradle Wrapper 与版本目录
├── build-android-lan.sh    # 按局域网/USB 调试地址构建 APK
├── start-backend.sh        # 后端启动脚本
└── README.md
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.8+
- MySQL 8.x
- Redis 6.x+
- Android Studio 或命令行 Android SDK
- Android 调试设备需要开启 USB 调试

## 数据库准备

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS tps
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

后端默认使用 JPA `ddl-auto=update`，并在启动时执行一组兼容迁移，自动补齐本地开发库里的新增字段和表。完整初始化 SQL 见：

```text
backend/src/main/resources/sql/init.sql
```

## 后端配置

主要配置文件：

```text
backend/src/main/resources/application.yml
```

常用环境变量：

```bash
TPS_DB_URL='jdbc:mysql://localhost:3306/tps?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
TPS_DB_USERNAME=root
TPS_DB_PASSWORD=root
TPS_UPLOAD_DIR=/home/source/My_github/TPS/img
TPS_PUBLIC_BASE_URL=http://192.168.33.96:8080
TPS_JWT_SECRET=tps-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm
```

说明：

- `TPS_UPLOAD_DIR` 是图片上传目录。
- `TPS_PUBLIC_BASE_URL` 用于把 `/img/xxx.jpg` 转换为手机可访问的绝对 URL。
- 开发环境默认验证码为 `1234`。
- 本地默认管理员账号由启动初始化逻辑创建，手机号 `18888888888`，密码 `admin123`。

## 启动后端

推荐使用项目脚本：

```bash
cd /home/source/My_github/TPS
./start-backend.sh
```

脚本会自动：

- 设置 MySQL 默认账号 `root/root`
- 创建图片上传目录
- 自动探测当前电脑 IP
- 生成 `TPS_PUBLIC_BASE_URL`
- 检查 8080 是否已被占用

如果需要强制指定平板或手机可访问的电脑 IP：

```bash
TPS_LAN_IP=192.168.33.96 ./start-backend.sh
```

如果 8080 已有后端在运行，先查看并结束旧进程：

```bash
pgrep -af 'spring-boot:run|TpsApplication|tps-backend'
kill <PID>
./start-backend.sh
```

后端验证：

```bash
curl -i http://127.0.0.1:8080/api/files/ping
curl -i http://127.0.0.1:8080/swagger-ui.html
```

局域网设备验证：

```text
http://电脑IP:8080/swagger-ui.html
http://电脑IP:8080/api/files/ping
```

## 后端构建与测试

```bash
cd backend
mvn -q -DskipTests package
```

运行测试：

```bash
cd backend
mvn test
```

生成的 `backend/target/`、JAR 包和测试报告不应提交到 Git。

## Android 构建

项目默认 Android API 地址来自 Gradle 属性：

- `TPS_API_BASE_URL`
- `TPS_API_FALLBACK_BASE_URLS`
- `TPS_WS_URL`
- `TPS_WS_FALLBACK_URLS`

### 局域网真机构建

确保手机/平板和电脑在同一网络，并确认电脑 IP：

```bash
ip -4 addr show
```

构建 APK：

```bash
cd /home/source/My_github/TPS
./build-android-lan.sh 192.168.33.96
```

输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到 USB 设备：

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### USB 反向代理调试

如果希望 Android 设备通过 USB 访问电脑本机后端，可以使用 `adb reverse`：

```bash
adb reverse tcp:8080 tcp:8080
./build-android-lan.sh 127.0.0.1
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

注意：Android 设备里的 `127.0.0.1` 是设备自身。只有执行 `adb reverse tcp:8080 tcp:8080` 后，设备访问 `127.0.0.1:8080` 才会转发到电脑的 `8080`。

### Android Studio 构建

用 Android Studio 打开项目根目录，等待 Gradle 同步完成后运行 `app` 模块即可。若真机无法访问后端，优先检查 `TPS_API_BASE_URL` 和后端 `TPS_PUBLIC_BASE_URL` 是否指向同一个可访问地址。

## 图片链路说明

上传接口：

```http
POST /api/files/upload
```

返回结构：

```json
{
  "url": "http://电脑IP:8080/img/xxx.jpg",
  "path": "/img/xxx.jpg"
}
```

商品图片、用户头像、订单商品封面、聊天会话商品封面都会统一返回绝对 URL。排查图片问题时先访问：

```bash
curl -i http://电脑IP:8080/api/files/ping
curl -i 'http://电脑IP:8080/api/files/resolve?path=/img/test.jpg'
```

## 常用接口

- `POST /api/auth/code`：获取验证码
- `POST /api/auth/register`：注册
- `POST /api/auth/login`：登录
- `GET /api/products`：商品列表
- `GET /api/products/{id}`：商品详情
- `POST /api/products`：发布商品
- `GET /api/products/my`：我的发布
- `PATCH /api/products/{id}/status`：修改商品状态
- `GET /api/favorites`：收藏列表
- `POST /api/orders`：创建订单
- `GET /api/orders/my`：我的订单
- `POST /api/messages/conversation`：创建或获取会话
- `POST /api/messages/{conversationId}`：发送消息
- `GET /api/history/products`：浏览历史
- `POST /api/feedback`：提交反馈
- `GET /api/admin/feedback`：管理员查看反馈

完整接口文档：

```text
http://localhost:8080/swagger-ui.html
```

## Git 提交规范

建议提交源码和配置：

- `app/src/**`
- `backend/src/**`
- `exchange/**` 中的源码/静态资源
- `docs/**`
- `gradle/**`
- `*.gradle.kts`
- `gradlew`、`gradlew.bat`
- `start-backend.sh`
- `build-android-lan.sh`
- `README.md`
- `.gitignore`

不要提交：

- `.gradle/`
- `.kotlin/`
- `.idea/`
- `.codex`
- `.claude/`
- `app/build/`
- `backend/target/`
- `img/`
- `backend/img/`
- `local.properties`
- APK、AAB、JAR、日志、数据库文件

## 推送到 GitHub

首次整理暂存区：

```bash
git reset
git add .gitignore README.md
git add build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle/
git add start-backend.sh build-android-lan.sh
git add backend/pom.xml backend/src/
git add app/build.gradle.kts app/src/
git add docs/
git add exchange/
git status --short
```

提交：

```bash
git commit -m "完善校园二手交易系统后端与Android客户端"
```

推送：

```bash
git push origin master
```

推送前请确认 `git status --short` 中没有 `build/`、`target/`、`img/`、`.gradle/`、`.idea/`、APK、JAR 等文件。
