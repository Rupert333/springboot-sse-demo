package com.example.ssedemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单事件模型类
 * 用于封装从上游系统接收到的订单回调信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 订单金额
     */
    private Double amount;
    
    /**
     * 事件发生时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 附加信息
     */
    private String message;
}
