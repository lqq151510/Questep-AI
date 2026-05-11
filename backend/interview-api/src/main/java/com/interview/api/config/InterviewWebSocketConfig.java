package com.interview.api.config;

import com.interview.api.websocket.InterviewWebSocketHandler;
import com.interview.api.websocket.JwtWebSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class InterviewWebSocketConfig implements WebSocketConfigurer {

    private final InterviewWebSocketHandler interviewWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor;
    private final String allowedOrigins;

    public InterviewWebSocketConfig(
            InterviewWebSocketHandler interviewWebSocketHandler,
            JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor,
            @Value("${app.cors.allowed-origins:http://127.0.0.1:3000,http://localhost:3000}") String allowedOrigins
    ) {
        this.interviewWebSocketHandler = interviewWebSocketHandler;
        this.jwtWebSocketHandshakeInterceptor = jwtWebSocketHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewWebSocketHandler, "/ws/interview")
                .setAllowedOrigins(parseAllowedOrigins())
                .addInterceptors(jwtWebSocketHandshakeInterceptor);
    }

    private String[] parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toArray(String[]::new);
    }
}
