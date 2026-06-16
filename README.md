# TPS 校园二手交易系统

TPS 是一个校园二手交易 APP，仓库内包含 Android 客户端、Spring Boot 后端和联调脚本。

## 前置需求

- JDK 17
- Maven 3.8 或更高版本
- MariaDB / MySQL，可本地访问 `localhost:3306`
- Android SDK 与 `adb`
- 已执行首次部署脚本创建数据库 `tps`

首次部署：

```bash
./deploy/first-deploy.sh
```

常用默认值：

- 数据库账号密码：`root / root`
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
- `manifest.webmanifest`、`package.json`、`db.json`、`package-lock.json` 这类严格 JSON 文件不能直接写注释，否则会破坏语法，因此本次未在这些文件内嵌注释。

# 项目文件夹结构

下面按当前仓库实际结构来讲。对 .git/objects、.gradle/9.0.0 这种工具内部子目录，我会按类别说明，不逐个展开，因为它们不是业务结构。

  顶层目录

- app/：Android 应用主模块，负责用户端界面、接口调用、聊天、商品浏览、下单等。
- backend/：后端服务模块，负责认证、商品、订单、消息、收藏、反馈、管理员接口等业务。
- docs/：课程文档目录，放需求、设计、测试、可追踪矩阵、操作手册和各种 UML/工程图。
- exchange/：另一套前端资源，偏 Web/混合应用形态，部分静态资源会被同步进 Android APK 的 assets。
- scripts/：项目辅助脚本目录，目前主要是测试数据生成脚本。
- file_docx/：原始 Word 材料目录，是最初的需求/功能说明来源，不参与程序运行。
- gradle/：Gradle 包装器和依赖版本目录，负责 Android 构建基础设施。
- img/：运行时图片上传目录，后端上传接口保存的图片会落在这里。
- .git/：Git 仓库元数据目录，记录提交历史、分支、索引等。
- .gradle/：Gradle 本地缓存目录，构建过程中自动生成。
- .kotlin/：Kotlin/Gradle 本地缓存目录。
- build/：根项目的构建输出目录。
- .agents/：本地代理/自动化工具相关目录，不属于业务代码。
- .codex/：本地 AI/代理工作目录，不属于业务代码。

  顶层关键文件

- README.md：项目入口说明，主要写前置需求、启动方法、联调方式和注意事项。
- build.gradle.kts：根 Gradle 构建脚本，放公共构建配置。
- settings.gradle.kts：声明项目有哪些模块，比如 app。
- gradle.properties：Gradle 全局参数配置。
- gradlew、gradlew.bat：Gradle Wrapper 启动脚本，保证不同机器用同一版本 Gradle。
- build-android-lan.sh：按当前局域网 IP 构建 Android 调试 APK。
- start-backend.sh：一键启动后端，自动探测 IP、准备环境变量。
- .gitignore：告诉 Git 哪些文件不应该提交。
- local.properties：本机 Android SDK 路径配置，只对你这台机器有效，通常不应提交。

  Android 客户端：app/

- app/build.gradle.kts：Android 模块构建脚本，负责 SDK 版本、依赖、BuildConfig 地址注入、静态资源同步。
- app/build/：Android 构建产物目录，比如 APK、中间编译文件，不是手写源码。
- app/src/：Android 真正源码目录。

  Android 源码：app/src/main/

- app/src/main/AndroidManifest.xml：Android 清单文件，声明应用入口、权限、基础组件信息。
- app/src/main/java/：Kotlin 源码目录。
- app/src/main/res/：Android 资源目录，放图标、主题、字符串等。
- app/src/main/res/mipmap-*：不同分辨率下的应用图标资源。
- app/src/main/res/values/：字符串、主题等基础资源定义。

  Android 代码主包：app/src/main/java/com/tps/

- MainActivity.kt：Android 入口 Activity，应用打开后从这里进入 Compose 界面。
- TpsApp.kt：Application 入口，负责全局初始化，比如 Hilt 依赖注入。
- data/：数据层目录，负责接口访问、DTO、WebSocket 通信。
- di/：依赖注入目录，集中创建 Retrofit、OkHttp、TokenManager 等单例。
- ui/：界面层目录，放各页面的 Compose UI 与导航。
- util/：工具类目录，放 token、URL 等通用工具。

  Android 数据层：app/src/main/java/com/tps/data/

- remote/：远程数据访问层，负责和后端 HTTP / WebSocket 通信。
- remote/api/：接口声明目录，ApiService.kt 定义了前端可以调用的后端 API。
- remote/dto/：数据传输对象目录，Dtos.kt 放接口请求和响应模型。
- remote/websocket/：实时通信目录，StompClient.kt 负责聊天 WebSocket/STOMP 连接。
- FallbackBaseUrlInterceptor.kt：多地址兜底拦截器，解决 USB、热点、模拟器地址切换问题。
- NetworkEndpointConfig.kt：统一管理主地址和备用地址。

  Android 依赖注入：app/src/main/java/com/tps/di/

- NetworkModule.kt：网络相关单例提供者，比如 OkHttp、Retrofit、Gson、TokenManager。

  Android 界面层：app/src/main/java/com/tps/ui/

- NavHost.kt：客户端导航总入口，定义登录页、首页、详情页、聊天页等路由关系。
- admin/：管理员端页面。
- auth/：登录、注册模块。
- home/：主界面/首页模块。
- message/：消息列表与聊天模块。
- order/：订单模块。
- product/：商品列表、详情、发布模块。
- profile/：个人中心模块。
- theme/：统一主题和通用组件。

  Android UI 子目录说明

- ui/admin/：管理员功能页面，包含用户管理、举报处理、商品管理等。
- ui/auth/：登录注册页面和对应状态管理。
- ui/home/：APP 主页和主框架。
- ui/message/：聊天列表、聊天页面、消息状态管理。
- ui/order/：订单列表与订单状态操作。
- ui/product/：商品首页、商品详情、发布商品、商品状态流转。
- ui/profile/：我的资料、我的商品、收藏、浏览历史、反馈。
- ui/theme/：视觉主题、卡片、空状态等可复用 UI 组件。

  Android 工具层：app/src/main/java/com/tps/util/

- TokenManager.kt：保存和读取登录 token、用户身份信息。
- UrlUtils.kt：处理图片地址、接口地址等 URL 兼容逻辑。

  后端：backend/

- backend/pom.xml：Maven 构建文件，定义后端依赖和打包方式。
- backend/src/：后端源码与资源目录。
- backend/target/：Maven 构建输出目录，不是手写源码。

  后端源码：backend/src/main/

- backend/src/main/java/：Java 业务源码。
- backend/src/main/resources/：配置文件、SQL、静态资源等。
- backend/src/main/resources/application.yml：公共配置文件。
- backend/src/main/resources/application-dev.yml：开发环境覆盖配置。
- backend/src/main/resources/sql/：数据库初始化 SQL。
- backend/src/main/resources/sql/init.sql：初始化表结构和基础数据的脚本。

  后端主包：backend/src/main/java/com/tps/

- TpsApplication.java：Spring Boot 启动入口。
- config/：后端配置层。
- controller/：接口控制层。
- dto/：请求/响应对象。
- entity/：数据库实体。
- exception/：业务异常定义。
- repository/：数据库访问层。
- security/：JWT 和鉴权逻辑。
- service/：核心业务逻辑层。
- websocket/：聊天 WebSocket 入口。

  后端配置层：backend/src/main/java/com/tps/config/

- SecurityConfig.java：Spring Security 配置，定义哪些接口放行、哪些要登录。
- WebSocketConfig.java：STOMP/WebSocket 配置。
- DataInitializer.java：启动时做兼容迁移和默认管理员初始化。
- GlobalExceptionHandler.java：统一异常处理，把异常转成标准 JSON 响应。

  后端控制层：backend/src/main/java/com/tps/controller/

- AuthController.java：登录、注册、验证码。
- ProductController.java：商品发布、列表、详情、上下架、擦亮等。
- OrderController.java：订单创建、支付、发货、确认收货等。
- MessageController.java：会话列表、历史消息、发送消息、已读标记。
- FavoriteController.java：收藏相关接口。
- BrowsingHistoryController.java：浏览历史接口。
- FeedbackController.java：用户反馈接口。
- NotificationController.java：通知接口。
- UserController.java：个人资料接口。
- FileController.java：图片上传、图片访问辅助接口。
- AdminController.java：管理员接口入口。

  后端 DTO：backend/src/main/java/com/tps/dto/

- ApiResponse.java：统一接口返回结构。
- PageResponse.java：分页返回结构。
- dto/auth/：登录注册相关请求/响应。
- dto/product/：商品请求/响应对象。
- dto/order/：订单与评价对象。
- dto/message/：会话与消息对象。
- dto/user/：用户资料对象。
- dto/feedback/：反馈对象。
- dto/file/：上传结果对象。
- dto/notification/：通知对象。
- dto/admin/：管理员视角的对象，比如举报数据。

  后端实体层：backend/src/main/java/com/tps/entity/

- User.java：用户表映射。
- Product.java：商品主表。
- ProductImage.java：商品图片表。
- Order.java：订单表。
- Message.java：聊天消息表。
- Conversation.java：会话表。
- Favorite.java：收藏表。
- BrowsingHistory.java：浏览历史表。
- Feedback.java：反馈表。
- Notification.java：通知表。
- Report.java：举报表。
- Review.java：评价表。

  后端数据访问层：backend/src/main/java/com/tps/repository/

- 这一层基本都是 JPA Repository。
- 功能是把“查数据库”这件事抽出来，比如查用户、查商品、查订单、查消息、查收藏。

  后端安全层：backend/src/main/java/com/tps/security/

- JwtUtil.java：生成和解析 JWT。
- JwtAuthFilter.java：每个请求进来先解析 token，再把用户身份放进安全上下文。

  后端业务层：backend/src/main/java/com/tps/service/

- AuthService.java：认证业务。
- ProductService.java：商品业务核心。
- OrderService.java：订单流转规则。
- MessageService.java：消息持久化、未读数、会话归一化。
- UserService.java：个人资料业务。
- FavoriteService.java：收藏业务。
- BrowsingHistoryService.java：浏览历史业务。
- FeedbackService.java：反馈业务。
- FileService.java：文件路径与绝对 URL 处理。
- AdminService.java：管理员业务聚合。

  后端 WebSocket：backend/src/main/java/com/tps/websocket/

- ChatController.java：聊天消息的 WebSocket 入口。
- ChatMessage.java：WebSocket 消息载荷模型。

  后端测试：backend/src/test/

- backend/src/test/java/com/tps/BackendIntegrationTest.java：后端集成测试。
- backend/src/test/resources/application-test.yml：测试环境配置。

  文档目录：docs/

- README.md：文档总览。
- requirements.md：需求分析文档。
- use-case-specification.md：用例规约。
- database-design.md：数据库设计说明。
- detailed-design.md：详细设计说明。
- api-contract.md：前后端接口契约。
- requirements-traceability-matrix.md：需求可追踪矩阵。
- test-plan.md：测试计划。
- test-cases.md：测试用例。
- operation-manual.md：操作手册。
- production-readiness.md：上线准备检查。
- backend-completion-plan.md：后端补全计划。
- development-plan.md：开发计划。

  图目录：docs/diagrams/

- 这个目录是软件工程课程图纸集合。
- 每张图通常有 3 份：.puml 是源文件，.png 是位图，.svg 是矢量图。
- 01-use-case.*：用例图。
- 02-system-context.*：系统上下文图。
- 03-architecture.*：系统架构图。
- 04-er-diagram.*：ER 图。
- 05-class-diagram.*：类图。
- 06-sequence-publish-product.*：发布商品时序图。
- 07-state-order.*：订单状态图。
- 08-navigation.*：页面导航图。
- docs/diagrams/README.md：图目录说明和渲染说明。

  Web/混合端目录：exchange/

- 这是另一套前端资源，不是 Android Compose 主界面。
- 它更像移动 Web 或混合端页面集合，有自己的 HTML、CSS、JS、PWA 配置和脚本。
- 项目里还把其中一部分静态资源同步进 Android 包内，作为内嵌 Web 资源使用。

  exchange/ 里的主要内容

- index.html：Web 入口页面。
- styles.css：Web 样式。
- app.js：前端主要交互逻辑。
- app-config.js：前端运行配置。
- server.js：本地预览服务。
- sw.js：Service Worker，支持缓存/离线能力。
- manifest.webmanifest：PWA 清单。
- icon.svg：图标资源。
- db.json：模拟数据文件。
- package.json、package-lock.json：Node 依赖和脚本定义。
- capacitor.config.json：Capacitor 混合应用配置。
- README.md：exchange 子项目说明。

  exchange/mobile-web/

- 这是 exchange 下的移动端 Web 版本。
- 作用是提供更贴近手机浏览体验的一套页面和样式。
- 里面的 index.html、app.js、styles.css、sw.js 与根 exchange 类似，但偏移动端适配。

  exchange/scripts/

- 放 Node 辅助脚本。
- api-smoke-test.mjs：接口冒烟测试。
- prepare-capacitor-web.mjs：准备 Capacitor Web 资源。
- reset-db.mjs：重置模拟数据。
- serve-preview.mjs：本地预览服务辅助脚本。

  脚本目录：scripts/

- seed-test-data.sh：自动生成测试账号、测试商品、收藏、举报和典型状态数据，方便真机联调。

  运行时数据目录：img/

- 这里不是源码目录。
- 它保存后端上传后的图片文件。
- 前端商品图、头像图最终很多都会通过这个目录对应的 URL 被访问到。

  原始材料目录：file_docx/

- MY校园淘二手.docx：项目总体说明。
- 交流与沟通.docx：聊天/沟通需求原始描述。
- 后台管理.docx：管理员功能原始描述。
- 商品发布与下架.docx：商品管理需求原始描述。
- 搜索与浏览.docx：搜索浏览需求原始描述。
- 评价与信用.docx：评价和信用体系需求原始描述。
- 这个目录的定位更像“需求输入材料”，不是程序运行的一部分。

  构建和缓存输出目录

- app/build/：Android 编译输出。
- backend/target/：Maven 打包输出。
- build/：根项目构建输出。
- .gradle/：Gradle 缓存。
- .kotlin/：Kotlin 编译缓存。
- 这些目录一般不应该作为业务代码阅读重点，也通常不提交。

# 答辩

1. 项目主题与核心创意
  项目主题是“MY校园淘二手”，也就是一个面向本校师生的校园二手交易平台。它要解决的问题很明确：现在校园里的二手交易大多依赖微信群、QQ群，信息分散、搜索困难、商品描述不规
  范、已售商品还会被反复询问，而且缺少评价机制，交易安全性也比较弱。

  我们的核心创意有三点。第一，平台聚焦“校园内部”这个场景，不做泛化电商，而是服务教材、电子产品、生活用品这类校园高频闲置物品交易。第二，平台强调“真实身份”，通过学号认证
  把用户限定在校内，降低校外人员混入和诈骗风险。第三，平台不是只做商品展示，而是做一个完整闭环：从商品发布、搜索筛选、站内沟通，到成交后的评价与信用，再到后台管理与违规
  处理，把交易效率和交易安全一起考虑进去。

  如果你现场要口述，可以直接这样说：
  “我们的项目主题是校园二手交易 APP。它主要解决校园二手交易中信息分散、难检索、缺少信用保障的问题。我们的核心创意是，把校园身份认证、商品交易、私信沟通、信用评价和后台治
  理整合到一个平台中，构建一个更安全、更高效、更适合校内场景的二手交易系统。”

  1. 需求文档与个人分工
  建议你这一部分按“总需求文档 + 5 个功能模块文档”来讲。

  先讲总需求文档：file_docx/MY校园淘二手.docx
  这份文档定义了整个项目的背景、问题、目标用户、核心功能和使用场景。它指出了平台要解决的 4 个核心痛点：信息分散、沟通成本高、缺少评价机制、已售信息滞后。它同时给出了系统
  的总体功能范围，包括用户模块、商品模块、搜索浏览、交易沟通、评价信用、后台管理。

  然后讲各功能模块分工。

- file_docx/交流与沟通.docx，负责人是黄阳阳。这个模块负责站内私信、留言意向、商品已售标记。它解决的是“买卖双方怎么联系”和“成交后商品状态怎么更新”的问题。对应的核心用例
    是发送私信、留言意向、标记商品已售。
- file_docx/后台管理.docx，负责人是段东波。这个模块负责管理员对平台内容和用户进行治理，主要包括管理商品、处理举报、管理用户。它解决的是平台运行后的秩序维护问题。
- file_docx/商品发布与下架.docx，负责人是徐海亮。这个模块负责商品发布、编辑、下架，以及管理员强制下架。它是交易流程的起点，决定了卖家能否规范地把商品信息发布出去。
- file_docx/搜索与浏览.docx，负责人是洪中轩。这个模块负责搜索商品、筛选商品、查看商品详情。它主要解决“买家如何快速找到自己需要的商品”这个问题。
- file_docx/评价与信用.docx，负责人是曹开显。这个模块负责交易完成后的互评和信用分展示。它的作用是建立用户信用体系，提升平台交易安全感。

  如果你要把这一段讲得更像答辩，可以直接这样说：
  “我们先根据总需求文档完成整体功能拆分，再把系统分成 5 个核心模块。黄阳阳负责交流与沟通模块，包括私信、留言和已售标记；段东波负责后台管理模块，包括商品治理、举报处理和
  用户管理；徐海亮负责商品发布与下架模块，包括发布、编辑、下架和管理员强制下架；洪中轩负责搜索与浏览模块，包括关键词搜索、条件筛选和商品详情展示；曹开显负责评价与信用模
  块，包括交易后互评和信用分展示。这样分工的好处是，每个人都围绕一个完整业务模块开展工作，职责边界比较清晰。”

  如果你自己是组长或者整合人，还可以补一句：
  “在以上分工基础上，我负责整体架构设计、前后端联调、数据库整合、测试和最终文档汇总，确保各模块最终能组成一个完整系统。”

  1. 问答环节
  下面这些是很可能被老师问到的问题，你可以直接背答案。

  Q：你们为什么要做校园二手交易，而不是普通电商？
  A：因为校园二手交易有很强的场景特征，交易对象主要是本校学生，商品类型集中在教材、电子产品和生活用品，交易方式也更偏向线下面交。所以我们做的不是泛电商，而是一个针对校园
  场景优化的平台。

  Q：你们项目最大的创新点是什么？
  A：最大的创新点不是某一个单独功能，而是把校园身份认证、二手交易、站内沟通、信用评价和后台治理整合成一个闭环系统，既关注效率，也关注安全。

  Q：为什么要强调学号认证？
  A：因为校内二手交易最重要的问题之一就是信任。学号认证能把用户限定在校内群体里，显著降低匿名交易和校外诈骗风险，这也是校园平台区别于普通二手平台的重要特征。

  Q：搜索与私信功能为什么重要？
  A：因为校园二手交易的核心痛点就是“找不到”和“沟通低效”。搜索解决的是信息检索问题，私信解决的是交易协商问题，这两个功能直接决定平台是否真正可用。

  Q：评价与信用模块的意义是什么？
  A：评价与信用模块能积累用户在平台上的交易信誉，让买家在选择卖家时有参考依据，也能反向约束用户行为，提高整体交易安全性。

  Q：为什么还要做后台管理？
  A：因为仅靠前台交易功能不能保证平台长期稳定运行。后台管理可以处理违规商品、恶意用户和举报信息，是平台治理能力的一部分，也是系统完整性的体现。

  Q：你们的需求是怎么拆分成个人分工的？
  A：我们是按业务模块拆分的，不是按技术细节拆分。这样每个人都能围绕一个完整用户场景去做需求分析、用例设计和功能实现，模块边界更清晰，也更便于后期集成。

  Q：这个系统的核心业务流程是什么？
  A：核心流程是卖家发布商品，买家搜索和筛选商品，进入详情页后通过私信或留言与卖家沟通，达成交易后卖家标记已售，最后双方互评并更新信用分。如果过程中出现问题，管理员可以介
  入处理。

  Q：如果后续继续完善，你们会优先做什么？
  A：我会优先完善统一认证、交易状态闭环、消息实时性、信用机制细化和后台治理能力，因为这些部分最直接影响用户体验和平台可信度。
