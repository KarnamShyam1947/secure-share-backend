package com.secure_share.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.secure_share.handlers.SignalingHandler;

@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    @SuppressWarnings("null")
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SignalingHandler(), "/signal").setAllowedOrigins("*");
    }
}
