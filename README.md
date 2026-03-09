# OpsAny 1:1 复刻工程

基于你要求的技术栈重建：

- 后端：`Java 8 + Spring Boot 2.7`
- 前端：`Vue 3 + TypeScript + Vite`
- 数据库：`MySQL`
- 缓存：`Redis`
- 消息队列：`RabbitMQ`

当前工作区已经收敛为两个主项目：

- `backend`：工作台聚合接口、登录、工单创建、Redis 会话、RabbitMQ 事件、MySQL 种子数据
- `frontend`：Vue 版工作台壳子与各菜单页面，界面结构按 OpsAny 工作台 1 比 1 迁移

## 当前完成度

- 已接入演示登录：`demo / 123456.coM`
- 已完成工作台、服务门户、工单管理、任务管理、值班、大屏、消息、订阅、流程管理、导航管理、系统设置等页面复刻
- 已实现工单创建接口：写入 MySQL，并通过 RabbitMQ 事件链路生成任务/消息；当 RabbitMQ 不可用时会走后备同步逻辑
- 前端已验证 `npm run build`
- 后端已验证 `mvnw.cmd -DskipTests package`

## 本机环境现状

- `Redis`：可用，`redis-cli ping` 返回 `PONG`
- `MySQL`：服务在运行，但当前未拿到可用账号密码；`root` 空密码无法登录
- `RabbitMQ`：服务在运行，但你本机之前检测到 `5672/15672` 未正常监听，建议先修复后再启用真实消费

## 启动前准备

1. 先在 MySQL 中创建数据库：

```sql
CREATE DATABASE opsany_replica CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 准备一个可用的 MySQL 账号，并设置环境变量。

PowerShell 示例：

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="opsany_replica"
$env:MYSQL_USERNAME="你的账号"
$env:MYSQL_PASSWORD="你的密码"
```

3. 如果本机 RabbitMQ 还没修好，建议先关闭消费者启动：

```powershell
$env:RABBITMQ_CONSUMER_ENABLED="false"
```

## 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

默认端口：`8080`

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

默认端口：`5173`

## 登录说明

- 前端地址：`http://localhost:5173`
- 演示账号：`demo`
- 演示密码：`123456.coM`
