
# 🌐 Server-Sent Events（SSE）入门与实战指南

## 1. 什么是 SSE？

**SSE（Server-Sent Events）** 是一种基于 HTTP 协议的服务端推送技术，允许服务端主动向浏览器发送数据。它使用浏览器原生的 `EventSource` 接口建立一个长连接，通过 `text/event-stream` 格式将消息源源不断推送给前端页面。

- 通讯方向：服务端 → 客户端（单向）
- 协议基础：HTTP（底层是 TCP）
- 应用场景：通知推送、进度反馈、数据订阅等

SSE 建立的是 HTTP 长连接，非常适合前端只需“被动接收”的场景，例如：

- 后台任务完成通知
- 保单状态更新
- 实时线索提醒

------

## 2. SSE 与 WebSocket 对比

| 特性       | 轮询              | WebSocket              | SSE                       |
| ---------- | ----------------- | ---------------------- | ------------------------- |
| 通讯方向   | 双向（请求-响应） | 双向通信               | 服务端 → 客户端（单向）   |
| 协议基础   | HTTP              | TCP + 自定义协议       | HTTP（text/event-stream） |
| 浏览器支持 | 全部支持          | 现代浏览器             | 现代浏览器（不支持 IE）   |
| 建立难度   | 简单              | 复杂                   | 简单                      |
| 网络兼容性 | 非常好            | 可能受限于代理或防火墙 | 非常好                    |
| 重连机制   | 无（需手写）      | 无（需手写）           | 自动重连                  |
| 使用场景   | 低实时、简单交互  | 高实时、复杂交互       | 实时通知、状态反馈        |

> ✅ 适合使用 SSE 的场景：只需要服务端向前端推送消息，无需前端向服务端频繁通信。

------

## 3. 项目实践场景：订单/通知系统

在实际项目中，我们使用 **Spring Boot 2.7.5 + React** 实现了一个基于 SSE 的订单状态实时通知系统。客户端建立长连接，后端在收到订单状态变更后将消息实时推送。

------

## 4. 后端实现（Spring Boot）

### 4.1 添加依赖（Gradle）

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

------

### 4.2 核心 Controller 示例（支持断点续传）

```java
@RestController
@RequestMapping("/sse")
public class SseController {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/connect")
    public SseEmitter connect(@RequestParam String clientId,
                              @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 设置连接超时 30 分钟
        emitters.put(clientId, emitter);

        emitter.onTimeout(() -> emitters.remove(clientId));
        emitter.onCompletion(() -> emitters.remove(clientId));

        if (lastEventId != null) {
            System.out.println("客户端希望补发 ID > " + lastEventId + " 的消息");
            // TODO: 查询数据库或缓存补发缺失消息
        }

        return emitter;
    }

    @PostMapping("/push")
    public void push(@RequestParam String clientId, @RequestParam String content) throws IOException {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            String messageId = String.valueOf(System.currentTimeMillis());
            emitter.send(SseEmitter.event()
                    .id(messageId)
                    .name("message")
                    .data(content));
        }
    }
}
```

------

## 5. 前端实现（HTML + JS）

使用浏览器原生的 `EventSource`，自动支持断线重连，支持 `Last-Event-ID`：

```html
<!DOCTYPE html>
<html lang="zh">
<head>
  <meta charset="UTF-8">
  <title>SSE 消息接收</title>
</head>
<body>
  <h2>实时订单通知</h2>
  <ul id="msg-list"></ul>

  <script>
    const clientId = 'user-001';

    function createEventSource(lastEventId = null) {
      let url = `/sse/connect?clientId=${clientId}`;
      const source = new EventSource(url);

      source.onopen = () => console.log("SSE连接已建立");

      source.onmessage = (event) => {
        console.log("收到消息：", event.data);
        const li = document.createElement("li");
        li.textContent = `消息（ID: ${event.lastEventId}）: ${event.data}`;
        document.getElementById("msg-list").appendChild(li);

        localStorage.setItem("lastEventId", event.lastEventId);
      };

      source.onerror = () => {
        console.warn("连接断开，准备重连...");
        source.close();
        const savedId = localStorage.getItem("lastEventId");
        setTimeout(() => createEventSource(savedId), 3000);
      };
    }

    createEventSource();
  </script>
</body>
</html>
```

------

## 6. SSE 核心机制说明

### ✅ 自动重连

浏览器原生支持，当连接断开时，每 3 秒自动尝试重连，无需手动干预。

### ✅ 消息续发（Last-Event-ID）

每条消息可通过 `.id(...)` 设置唯一 ID，浏览器自动记住，断线重连后发送请求头 `Last-Event-ID`，服务端可补发中断期间的消息。

### ✅ 连接管理建议

- 使用 `Map<clientId, SseEmitter>` 管理连接
- clientId 建议为用户 ID、浏览器 sessionId 等唯一值
- 清理已断开的连接，防止内存泄漏

------

## 7. SSE 心跳机制（可选）

虽然浏览器会自动维持连接，但为了防止中间网络设备关闭空闲连接，可定期发送空消息：

```java
emitter.send(":\n\n"); // SSE 注释行，作为心跳
```

建议使用定时任务每 30 秒推送一次。

------

## 8. 和轮询对比资源消耗

| 指标       | 轮询（5s）       | SSE（心跳 30s）    |
| ---------- | ---------------- | ------------------ |
| 请求频率   | 每 5 秒一次      | 长连接，无重复请求 |
| 服务端负载 | 高（频繁连接）   | 低（只需保持连接） |
| 消息延迟   | 高（取决于频率） | 低（几乎实时）     |
| 带宽占用   | 高               | 极低（心跳+消息）  |

------

## 9. 总结

✅ SSE 是一种轻量、简单、易部署的服务端推送方案，适合：

- 单向通知
- 实时状态更新
- 中低频消息推送

相比 WebSocket，SSE 上手更快、兼容性好，尤其适合在已有 Spring Boot 项目中快速集成。
