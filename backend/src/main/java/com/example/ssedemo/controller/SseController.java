package com.example.ssedemo.controller;

import com.example.ssedemo.model.OrderEvent;
import com.example.ssedemo.service.ISseEmitterService;
import com.example.ssedemo.service.OrderCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SSE控制器
 * 提供SSE连接端点和订单回调接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 允许跨域请求，实际生产环境应限制为特定域名
public class SseController {

//    private final SseEmitterService sseEmitterService;
    private final ISseEmitterService sseEmitterService;
    private final OrderCallbackService orderCallbackService;
    
    /**
     * 建立SSE连接
     * 前端通过该接口建立SSE连接，接收实时订单更新
     *
     * @param clientId 客户端ID，如果为空则自动生成
     * @return SseEmitter实例
     */
    @GetMapping(path = "/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(required = false) String clientId) {
        // 如果客户端没有提供ID，则生成一个唯一ID
        String id = (clientId != null && !clientId.isEmpty()) ? clientId : UUID.randomUUID().toString();
        log.info("客户端 {} 请求建立SSE连接", id);

        return sseEmitterService.createEmitter(id);
    }
    
    /**
     * 接收订单回调
     * 上游系统通过该接口发送订单状态更新
     *
     * @param orderId 订单ID
     * @param status 订单状态
     * @param amount 订单金额
     * @param message 附加信息
     * @return 处理结果
     */
    @PostMapping("/order/callback")
    public ResponseEntity<Map<String, Object>> orderCallback(
            @RequestParam String orderId,
            @RequestParam String status,
            @RequestParam Double amount,
            @RequestParam(required = false) String message) {
        
        log.info("接收到订单回调: orderId={}, status={}, amount={}", orderId, status, amount);
        
        // 处理订单回调并通过SSE推送给前端
        orderCallbackService.handleOrderCallback(orderId, status, amount, message);
        
        // 返回处理成功的响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "订单回调处理成功");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 模拟订单回调
     * 用于测试SSE功能，生成模拟订单事件并广播
     *
     * @param orderId 订单ID，如果为空则自动生成
     * @return 模拟的订单事件
     */
    @GetMapping("/order/simulate")
    public ResponseEntity<OrderEvent> simulateOrderCallback(
            @RequestParam(required = false) String orderId) {
        
        // 如果没有提供订单ID，则生成一个
        String id = (orderId != null && !orderId.isEmpty()) ? orderId : "ORDER-" + UUID.randomUUID().toString().substring(0, 8);
        
        // 生成模拟订单事件并广播
        OrderEvent event = orderCallbackService.simulateOrderCallback(id);
        
        return ResponseEntity.ok(event);
    }

    /**
     * 获取SSE连接状态
     * 返回当前活跃的SSE连接数量
     *
     * @return 连接状态信息
     */
    @GetMapping("/sse/status")
    public ResponseEntity<Map<String, SseEmitter>> getSseStatus() {
        return ResponseEntity.ok(sseEmitterService.getActiveEmitters());
    }
}
