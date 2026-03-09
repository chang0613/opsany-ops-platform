# OpsAny 1:1 复刻工程

基于以下技术栈重建：

- 后端：`Java 8 + Spring Boot 2.7`
- 前端：`Vue 3 + TypeScript + Vite`
- 数据库：`MySQL`
- 缓存：`Redis`
- 消息队列：`RabbitMQ`

当前工作区：

- `backend`：登录、工作台聚合、工单创建、任务/消息联动
- `frontend`：Vue 版工作台界面与导航页面

## 默认启动

第一次拉代码后，后端默认直接连接共享开发环境：

- MySQL：`172.16.22.50:3306/workflow`
- Redis：`172.16.22.43:6379/9`

默认启动命令：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

前端启动命令：

```powershell
cd frontend
npm install
npm run dev
```

访问地址：

- 前端：`http://localhost:5173/login`
- 后端：`http://localhost:8080`
- 演示账号：`demo`
- 演示密码：`123456.coM`

说明：

- 默认模式会使用共享 MySQL 和共享 Redis
- RabbitMQ 默认不阻塞启动，消费者默认关闭
- 工单事件默认走同步回填，保证本地开发可直接使用

## 本地离线模式

如果需要完全脱离服务器资源，可切到 `local` profile：

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
cd backend
.\mvnw.cmd spring-boot:run
```

`local` 模式下会使用：

- `H2` 内存数据库
- 内存会话
- 关闭 RabbitMQ 消费者

## 覆盖默认配置

如果需要覆盖共享环境配置，可以使用环境变量：

```powershell
$env:MYSQL_HOST="172.16.22.50"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="workflow"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="1qaz@WSX"
$env:REDIS_HOST="172.16.22.43"
$env:REDIS_PORT="6379"
$env:REDIS_DATABASE="9"
$env:REDIS_PASSWORD="redis-22043"
```

## 当前实现

- 已接入演示登录：`demo / 123456.coM`
- 已完成工作台、服务门户、工单管理、任务管理、值班、大屏、消息、订阅、流程管理、导航管理、系统设置等页面复刻
- 已实现工单创建接口：写入数据库，并生成对应任务与消息
