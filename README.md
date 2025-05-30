
# SpringBoot SSE Demo

这是一个使用Server-Sent Events (SSE)技术实现的实时订单通知系统示例项目。项目展示了如何使用SpringBoot和React构建一个基于SSE的实时通信应用。

关于sse介绍请参考 [SSE技术介绍](docs/SSE技术介绍.md)

## 技术栈

### 后端
- SpringBoot 2.7.5
- Spring Web
- Lombok
- Maven

### 前端
- React
- Ant Design
- EventSource API

## 功能特性

- 实时订单状态推送
- 心跳机制保持连接
- 自动重连机制
- 订单状态可视化
- 模拟订单回调测试
- 连接状态监控

## 项目结构

```
├── backend/          # SpringBoot后端项目
├── frontend/         # React前端项目
└── docs/             # 项目文档
```

## 快速开始

### 后端启动

1. 进入backend目录
```bash
cd backend
```

2. 使用Maven构建项目
```bash
./mvnw clean install
```

3. 运行SpringBoot应用
```bash
./mvnw spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 前端启动

1. 进入frontend目录
```bash
cd frontend
```

2. 安装依赖
```bash
npm install
```

3. 启动开发服务器
```bash
npm start
```

前端应用将在 http://localhost:3000 启动

## 使用说明

1. 启动后端和前端服务
2. 打开浏览器访问 http://localhost:3000
3. 点击"连接"按钮建立SSE连接
4. 使用"模拟订单回调"按钮测试订单通知功能
5. 观察订单更新列表和连接状态信息

## 核心功能实现

### 后端实现
- `ISseEmitterService`: 管理SSE连接和消息推送
- `OrderCallbackService`: 处理订单回调和消息广播
- `SseController`: 提供SSE连接和订单回调接口

### 前端实现
- `OrderNotification`: React组件，处理SSE连接和消息展示
- 心跳检测机制确保连接可靠性
- 使用Ant Design组件优化用户界面

## 文档

详细的技术文档请参考 [SSE技术介绍](docs/SSE技术介绍.md)

## 注意事项

- 确保后端服务器支持CORS
- 建议使用现代浏览器运行前端应用
- 生产环境部署时需要适当配置超时时间和重试策略

## 开发环境要求

- JDK 8+
- Node.js 12+
- Maven 3.6+

        