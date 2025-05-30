package com.example.ssedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 启用异步处理，对于SSE消息推送很重要
public class SseDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SseDemoApplication.class, args);
    }
}
