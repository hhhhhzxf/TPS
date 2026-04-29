# TPS 文档中心

**版本：** v1.1  
**日期：** 2026-04-28  
**项目定位：** Android 移动端二手交易平台 + Spring Boot 后端服务

---

## 1. 文档目标

本文档目录用于统一管理 TPS 的需求、计划、接口契约、后端完善任务、Android 联调约定和生产发布准备。

当前阶段的工作重点是：

1. 先完善后端，确保 Android APP 有稳定接口可接入。
2. 前端 / APP UI 可并行做静态页面和 Mock 数据。
3. 后端接口冻结后，再进入 Android 真机联调。
4. 最终目标是在安卓手机上可稳定完成核心交易链路。

---

## 2. 文档架构

| 文档 | 用途 | 主要读者 |
|------|------|----------|
| `requirements.md` | 产品需求、功能范围、非功能要求 | 产品 / 后端 / APP |
| `development-plan.md` | 总体开发阶段、技术选型、历史计划 | 全体 |
| `backend-completion-plan.md` | 后端现状、缺口、优先级、完成标准 | 后端 / Claude Code |
| `api-contract.md` | REST / WebSocket 接口契约、响应格式、错误码 | 后端 / APP / Gemini |
| `android-integration-guide.md` | Android 真机联调、网络、图片、WebSocket、鉴权约定 | APP / 后端 |
| `production-readiness.md` | 生产环境配置、安全、部署、测试、验收清单 | 后端 / 运维 |

---

## 3. 当前执行策略

### 3.1 后端优先

后端必须先完成以下能力：

- 认证、JWT、角色权限、封禁状态校验
- 商品、图片、收藏、搜索、分页
- 消息、会话、WebSocket 身份校验
- 订单状态机、并发购买防护、通知
- 举报、评价、管理员闭环
- Swagger / OpenAPI 文档
- Android 真机可访问的 API、图片、WebSocket 地址

### 3.2 APP 并行

Gemini 可以先完成：

- 登录 / 注册页面
- 首页 / 商品详情 / 发布商品页面
- 我的 / 收藏 / 订单页面
- 聊天列表 / 聊天页
- 管理员页面静态结构

在后端接口冻结前，APP 使用 Mock 数据，不直接绑定不稳定接口。

---

## 4. 开发分工

| 方向 | 推荐执行者 | 责任 |
|------|------------|------|
| 后端代码 | Claude Code | 实现接口、修复安全问题、补齐业务闭环 |
| Android UI | Gemini | 页面设计、Compose UI、交互状态、Mock 数据 |
| 联调验收 | 后端 + APP | 按接口契约逐模块联调 |
| 计划决策 | 项目负责人 | 控制范围、确认优先级、验收发布 |

---

## 5. 近期优先级

| 优先级 | 工作项 | 输出 |
|--------|--------|------|
| P0 | 后端认证与权限加固 | 登录稳定、权限可靠 |
| P0 | Android 真机访问方案 | 手机可访问 API / 图片 / WebSocket |
| P0 | 商品与图片链路稳定 | 首页、详情、发布可联调 |
| P0 | 订单状态机加固 | 交易闭环可靠 |
| P1 | 消息和 WebSocket 安全 | 聊天可用且不可伪造身份 |
| P1 | 评价 / 举报 / 通知 | 平台闭环完整 |
| P2 | 生产部署与监控 | 可上线运行 |

---

## 6. 文档维护规则

- 需求变化先更新 `requirements.md`。
- 后端任务变化更新 `backend-completion-plan.md`。
- 接口字段变化必须同步更新 `api-contract.md`。
- Android 联调问题沉淀到 `android-integration-guide.md`。
- 上线前检查 `production-readiness.md`。
