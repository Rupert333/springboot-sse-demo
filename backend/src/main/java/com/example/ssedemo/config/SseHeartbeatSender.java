package com.example.ssedemo.config;

import com.example.ssedemo.service.ISseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 心跳发送器
 * 负责定期向所有SSE客户端发送心跳消息
 * 使用Java原生的ScheduledExecutorService代替Spring的@Scheduled注解
 */
@Component
@Slf4j
public class SseHeartbeatSender {

    private final ISseEmitterService sseEmitterService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SseHeartbeatSender(ISseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * 初始化心跳发送任务
     * 使用Java原生的ScheduledExecutorService，每30秒发送一次心跳
     */
    @PostConstruct
    public void init() {
        log.info("初始化SSE心跳发送器");
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 10, 30, TimeUnit.SECONDS);
    }

    /**
     * 发送心跳
     * 异步执行，避免阻塞调度线程
     */
    @Async
    public void sendHeartbeat() {
        try {
            log.debug("发送SSE心跳");
            sseEmitterService.sendHeartbeat();
        } catch (Exception e) {
            log.error("发送SSE心跳失败", e);
        }
    }
    
    /**
     * 手动触发心跳发送
     * 可通过API端点调用此方法
     */
    public void triggerHeartbeat() {
        sendHeartbeat();
    }
}
