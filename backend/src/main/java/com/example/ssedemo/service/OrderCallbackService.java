package com.example.ssedemo.service;

import com.example.ssedemo.enums.SseEventEnum;
import com.example.ssedemo.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单回调处理服务
 * 负责处理从上游系统接收到的订单回调，并通过SSE推送给前端
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCallbackService {

    private final ISseEmitterService sseEmitterService;
    
    /**
     * 处理订单回调
     * 接收上游系统的订单回调信息，并通过SSE广播给所有连接的客户端
     *
     * @param orderId 订单ID
     * @param status 订单状态
     * @param amount 订单金额
     * @param message 附加信息
     */
    public void handleOrderCallback(String orderId, String status, Double amount, String message) {
        log.info("收到订单回调: orderId={}, status={}, amount={}", orderId, status, amount);
        
        // 构建订单事件对象
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .status(status)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .message(message)
                .build();
        
        // 异步广播订单事件给所有客户端
        broadcastOrderEvent(event);
    }
    
    /**
     * 异步广播订单事件
     * 使用@Async注解确保消息发送不会阻塞主业务流程
     *
     * @param event 订单事件
     */
    @Async
    public void broadcastOrderEvent(OrderEvent event) {
        log.info("开始广播订单事件: {}", event);
        sseEmitterService.broadcastToAll(SseEventEnum.ORDER_UPDATE.getEventName(), event);
    }
    
    /**
     * 模拟订单回调
     * 用于测试SSE功能，生成模拟订单事件并广播
     *
     * @param orderId 订单ID
     * @return 生成的订单事件
     */
    public OrderEvent simulateOrderCallback(String orderId) {
        String[] statuses = {"CREATED", "PAID", "PROCESSING", "SHIPPED", "DELIVERED"};
        String status = statuses[(int) (Math.random() * statuses.length)];
        Double amount = Math.round(Math.random() * 10000) / 100.0;
        
        OrderEvent event = OrderEvent.builder()
                .orderId(orderId)
                .status(status)
                .amount(amount)
                .timestamp(LocalDateTime.now())
                .message("模拟订单回调")
                .build();
        
        // 异步广播订单事件
        broadcastOrderEvent(event);
        
        return event;
    }
}
