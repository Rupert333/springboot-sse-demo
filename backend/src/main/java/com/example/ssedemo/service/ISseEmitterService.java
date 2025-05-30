package com.example.ssedemo.service;

import com.example.ssedemo.enums.SseEventEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ISseEmitterService {

    /**
     * 存储所有客户端的SSE连接
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private static final Long TIME_OUT = 60 * 60 * 1000L;

    /**
     * 创建新的SSE连接
     *
     * @param clientId 客户端ID，用于标识不同的连接
     * @return SseEmitter实例
     */
    public SseEmitter createEmitter(String clientId) {
        //  移除已存在的相同ID的连接
        removeSseEmitter(clientId);

        // 创建新实例，并注册回调函数
        SseEmitter emitter = new SseEmitter(TIME_OUT);
        registerCallbacks(clientId, emitter);

        // 创建新的SSE连接
        emitters.put(clientId, emitter);
        log.info("创建了新的SSE连接: {}", clientId);

        // 发送连接成功事件
        if (!safeSend(emitter, clientId, SseEventEnum.CONNECT.getEventName(), SseEventEnum.CONNECT.getData())) {
            emitters.remove(clientId);
        }
        return emitter;
    }


    /**
     * 向指定客户端发送事件
     *
     * @param clientId 客户端ID
     * @param event    事件
     */
    public void sendToClient(String clientId, String eventName, Object event) {
        SseEmitter emitter = this.emitters.get(clientId);
        safeSend(emitter, clientId, eventName, event);
    }

    /**
     * 向所有客户端广播事件
     */
    public void broadcastToAll(String eventName, Object data) {
        broadcast(eventName, data);
    }

    /**
     * 向所有客户端发送心跳消息
     * 保持连接活跃，防止超时断开
     */
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return; // 没有活跃连接，不需要发送心跳
        }

        log.debug("开始向 {} 个客户端发送心跳", emitters.size());
        broadcast(SseEventEnum.HEARTBEAT.getEventName(), SseEventEnum.HEARTBEAT.getData());
    }


    /**
     * 获取当前连接数
     *
     * @return 当前连接的客户端数量
     */
    public Map<String, SseEmitter> getActiveEmitters() {
        return this.emitters;
    }


    // ------------下面为私有方法------------


    private boolean safeSend(SseEmitter emitter, String clientId, String eventName, Object data) {
        if (ObjectUtils.isEmpty(emitter)) {
            log.warn("客户端 {} 的 SSEEmitter 已经被关闭，无法发送事件 {}", clientId, eventName);
            return false;
        }


        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            log.info("向客户端 {} 发送事件 [{}]: {}", clientId, eventName, data);
            return true;
        } catch (IOException e) {
            log.error("向客户端 {} 发送事件 [{}] 失败", clientId, eventName, e);
            emitter.completeWithError(e);
            return false;
        }
    }

    /**
     * 向所有客户端广播事件
     */
    private void broadcast(String eventName, Object data) {
        List<String> deadClients = new ArrayList<>();
        emitters.forEach((clientId, emitter) -> {
            if (!safeSend(emitter, clientId, eventName, data)) {
                deadClients.add(clientId);
            }
        });
        deadClients.forEach(emitters::remove);
    }

    /**
     * 注册回调函数，用于处理连接完成、超时和错误事件
     *
     * @param clientId 客户端ID
     * @param emitter  SSEEmitter实例
     */
    private void registerCallbacks(String clientId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: {}", clientId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE连接超时: {}", clientId);
            emitter.complete();
            emitters.remove(clientId);
        });

        emitter.onError(ex -> {
            log.error("SSE连接出错: {}", clientId, ex);
            emitter.complete();
            emitters.remove(clientId);
        });
    }


    /**
     * 移除已存在的相同ID的连接
     *
     * @param clientId
     * @author yangyg
     * @date 2025/5/30 3:49 PM
     */
    private void removeSseEmitter(String clientId) {
        SseEmitter existingEmitter = emitters.remove(clientId);

        if (ObjectUtils.isEmpty(existingEmitter)) {
            return;
        }

        existingEmitter.complete();
        log.info("关闭已存在的SSE连接: {}", clientId);
    }
}
