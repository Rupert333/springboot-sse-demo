package com.example.ssedemo.enums;


import lombok.Getter;

@Getter
public enum SseEventEnum {

    // 基础事件
    CONNECT("CONNECT", "创建连接", "连接成功"),
    HEARTBEAT("HEARTBEAT", "心跳连接", "ping"),

    // 业务事件
    ORDER_UPDATE("ORDER_UPDATE", "订单更新", "订单更新"),


    ;

    private String eventName;
    private String name;
    private String data;

    SseEventEnum(String eventName, String name, String data) {
        this.eventName = eventName;
        this.name = name;
        this.data = data;
    }

    SseEventEnum(String eventName, String name) {
        this.eventName = eventName;
        this.name = name;
    }
}
