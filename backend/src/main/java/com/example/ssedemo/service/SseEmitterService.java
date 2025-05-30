//package com.example.ssedemo.service;
//
//import com.example.ssedemo.model.OrderEvent;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * SSE事件发送服务
// * 负责管理客户端连接和向客户端推送消息
// */
//@Service
//@Slf4j
//public class SseEmitterService {
//
//    /**
//     * 存储所有客户端的SSE连接
//     * 使用ConcurrentHashMap保证线程安全
//     */
//    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
//
//    /**
//     * 创建新的SSE连接
//     *
//     * @param clientId 客户端ID，用于标识不同的连接
//     * @return SseEmitter实例
//     */
//    public SseEmitter createEmitter(String clientId) {
//        // 如果已存在相同clientId的连接，先关闭它
//        SseEmitter existingEmitter = this.emitters.get(clientId);
//        if (existingEmitter != null) {
//            existingEmitter.complete();
//            this.emitters.remove(clientId);
//            log.info("关闭已存在的SSE连接: {}", clientId);
//        }
//
//        // 设置超时时间为1小时，实际使用时可根据需求调整
//        SseEmitter emitter = new SseEmitter(3600000L);
//
//        // 注册连接完成时的回调
//        emitter.onCompletion(() -> {
//            log.info("SSE连接完成: {}", clientId);
////            this.emitters.remove(clientId);
//        });
//
//        // 注册连接超时时的回调
//        emitter.onTimeout(() -> {
//            log.info("SSE连接超时: {}", clientId);
//            emitter.complete();
//            this.emitters.remove(clientId);
//        });
//
//        // 注册连接出错时的回调
//        emitter.onError(ex -> {
//            log.error("SSE连接出错: {}", clientId, ex);
//            emitter.complete();
//            this.emitters.remove(clientId);
//        });
//
//        // 存储新创建的连接
//        this.emitters.put(clientId, emitter);
//        log.info("创建了新的SSE连接: {}", clientId);
//
//        // 发送初始连接成功消息
//        try {
//            emitter.send(SseEmitter.event()
//                    .name("CONNECT")
//                    .data("连接成功"));
//        } catch (IOException e) {
//            log.error("发送初始连接消息失败", e);
//            emitter.completeWithError(e);
//        }
//
//        return emitter;
//    }
//
//    /**
//     * 向指定客户端发送订单事件
//     *
//     * @param clientId 客户端ID
//     * @param event 订单事件
//     */
//    public void sendToClient(String clientId, OrderEvent event) {
//        SseEmitter emitter = this.emitters.get(clientId);
//        if (emitter != null) {
//            try {
//                emitter.send(SseEmitter.event()
//                        .name("ORDER_UPDATE")
//                        .data(event));
//                log.info("向客户端 {} 发送了订单事件: {}", clientId, event);
//            } catch (IOException e) {
//                log.error("向客户端 {} 发送订单事件失败", clientId, e);
//                emitter.completeWithError(e);
//                this.emitters.remove(clientId);
//            }
//        } else {
//            log.warn("客户端 {} 不存在或已断开连接", clientId);
//        }
//    }
//
//    /**
//     * 向所有客户端广播订单事件
//     *
//     * @param event 订单事件
//     */
//    public void broadcastToAll(OrderEvent event) {
//        List<String> deadEmitters = new ArrayList<>();
//
//        this.emitters.forEach((clientId, emitter) -> {
//            try {
//                emitter.send(SseEmitter.event()
//                        .name("ORDER_UPDATE")
//                        .data(event));
//                log.info("向客户端 {} 广播了订单事件: {}", clientId, event);
//            } catch (IOException e) {
//                log.error("向客户端 {} 广播订单事件失败", clientId, e);
//                deadEmitters.add(clientId);
//                emitter.completeWithError(e);
//            }
//        });
//
//        // 移除发送失败的连接
//        deadEmitters.forEach(this.emitters::remove);
//    }
//
//    /**
//     * 向所有客户端发送心跳消息
//     * 保持连接活跃，防止超时断开
//     */
//    public void sendHeartbeat() {
//        if (emitters.isEmpty()) {
//            return; // 没有活跃连接，不需要发送心跳
//        }
//
//        log.debug("开始向 {} 个客户端发送心跳", emitters.size());
//        List<String> deadEmitters = new ArrayList<>();
//
//        this.emitters.forEach((clientId, emitter) -> {
//            try {
//                emitter.send(SseEmitter.event()
//                        .name("HEARTBEAT")
//                        .data("ping"));
//                log.debug("向客户端 {} 发送了心跳", clientId);
//            } catch (IOException e) {
//                log.error("向客户端 {} 发送心跳失败", clientId, e);
//                deadEmitters.add(clientId);
//                emitter.completeWithError(e);
//            }
//        });
//
//        // 移除发送失败的连接
//        if (!deadEmitters.isEmpty()) {
//            log.info("移除 {} 个失效的连接", deadEmitters.size());
//            deadEmitters.forEach(this.emitters::remove);
//        }
//    }
//
//    /**
//     * 获取当前sseEmitter集合
//     *
//     * @return 当前连接的客户端数量
//     */
//    public Map<String, SseEmitter> getAllEmitters() {
//        return this.emitters;
//    }
//}
