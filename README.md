# TPS 校园二手交易系统

TPS 是一个校园二手交易项目，包含 Android 客户端、Spring Boot 后端和本地联调脚本。

## 运行

- JDK 17
- Maven 3.8 或更高版本
- MariaDB / MySQL，可本地访问 `localhost:3306`
- Android SDK 与 `adb`
- 已执行首次部署脚本创建数据库 `tps`

首次部署：

```bash
./deploy/first-deploy.sh
```

Android：

```bash
cd /home/hzxf/project/exchangepro/TPS
./build-android-lan.sh 192.168.93.171
```

## 默认账号

- 管理员手机号：`18888888888`
- 管理员密码：`admin123`
- 开发验证码：`1234`
- 默认管理员：`18888888888 / admin123`

## 使用教程

### 1. 首次部署数据库

```bash
TPS_DB_USERNAME=root TPS_DB_PASSWORD=YOU_DATABASE_PASSWD ./deploy/first-deploy.sh
```

脚本会导入 [deploy/database/tps-init.sql](deploy/database/tps-init.sql)，并准备上传目录 `img/`。

### 2. 启动后端

```bash
cd /home/source/myGtihub/TPS
TPS_DB_USERNAME=root TPS_DB_PASSWORD='root' TPS_LAN_IP=<你的电脑IP> ./start-backend.sh
```

或者简易启动，

```bash
TPS_LAN_IP=<你电脑IP> ./start-backend.sh
```

启动后可检查：

```bash
curl -i http://127.0.0.1:8080/api/files/ping
```

浏览器或平板可直接访问：

```text
http://<你的电脑IP>:8080/swagger-ui.html
```

### 3. 构建 Android 调试包

局域网真机：

```bash
./build-android-lan.sh <你的电脑IP>
```

USB 调试：

```bash
adb reverse tcp:8080 tcp:8080
./build-android-lan.sh 127.0.0.1
```

如果命令行构建提示找不到 SDK，请在项目根目录配置 `local.properties`，或者导出 `ANDROID_HOME`。

### 4. 安装 APK

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 5. 生成测试数据

后端启动后可执行：

```bash
BASE_URL=http://127.0.0.1:8080 ./scripts/seed-test-data.sh
```

它会创建测试账号、商品、收藏、举报和部分状态样例。

## 注意事项

- 热点、普通 Wi-Fi、USB 反向代理对应的地址不同；换网络后通常需要重新执行 `./build-android-lan.sh <新的IP>`。
- `start-backend.sh` 会自动推断局域网地址；如果推断错误，手动传 `TPS_LAN_IP=<IP>`。
- 后端服务默认占用 `8080`；若端口已被占用，先结束旧进程再重启。
- 聊天优先走 WebSocket，发送失败时客户端会退回 REST 兜底，但前提仍然是后端保持运行。
