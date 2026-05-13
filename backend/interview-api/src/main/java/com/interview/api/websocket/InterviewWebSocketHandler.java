package com.interview.api.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.application.dto.ChatMessage;
import com.interview.application.dto.ChatRequest;
import com.interview.application.service.ChatApplicationService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InterviewWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(InterviewWebSocketHandler.class);
    private static final int MAX_CONTEXT_MESSAGES = 20;

    private final ObjectMapper objectMapper;
    private final ChatApplicationService chatApplicationService;
    private final MeterRegistry meterRegistry;

    public InterviewWebSocketHandler(
            ObjectMapper objectMapper,
            ChatApplicationService chatApplicationService,
            MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.chatApplicationService = chatApplicationService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sendEvent(session, "connected", "WebSocket connected", null);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        meterRegistry.counter(
                "ws_disconnect_total",
                "close_code", String.valueOf(status.getCode()),
                "reason", safeReason(status.getReason())
        ).increment();
        super.afterConnectionClosed(session, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            sendError(session, "UNAUTHORIZED", "用户身份无效，请重新登录。", null);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }

        WsInterviewRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), WsInterviewRequest.class);
        } catch (Exception ex) {
            sendError(session, "BAD_PAYLOAD", "消息格式不正确，请检查 JSON 结构。", null);
            return;
        }

        if (request == null || !StringUtils.hasText(request.message())) {
            sendError(session, "EMPTY_MESSAGE", "消息不能为空。", null);
            return;
        }

        try {
            ChatRequest chatRequest = new ChatRequest(
                    enrichMessageForInterview(request),
                    trimContext(toChatContext(request.context()))
            );

            StringBuilder fullReply = new StringBuilder();
            chatApplicationService.chatStream(userId, chatRequest, token -> {
                try {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("token", token);
                    payload.put("timestamp", Instant.now().toString());
                    sendEvent(session, "chat_token", null, payload);
                    fullReply.append(token);
                } catch (IOException e) {
                    log.warn("Failed to send token to session {}: {}", session.getId(), e.getMessage());
                }
            });

            Map<String, Object> donePayload = new HashMap<>();
            donePayload.put("fullReply", fullReply.toString());
            donePayload.put("timestamp", Instant.now().toString());
            sendEvent(session, "chat_done", "ok", donePayload);
        } catch (Exception ex) {
            log.warn("WebSocket chat failed for session {}: {}", session.getId(), ex.getMessage());
            try {
                sendError(session, "CHAT_FAILED", ex.getMessage(), null);
            } catch (IOException ignored) {
                // session may already be closed
            }
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        Object rawUserId = session.getAttributes().get(JwtWebSocketHandshakeInterceptor.ATTR_USER_ID);
        if (rawUserId instanceof Long value) {
            return value;
        }
        if (rawUserId instanceof Integer value) {
            return value.longValue();
        }
        if (rawUserId instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String enrichMessageForInterview(WsInterviewRequest request) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.position()) || request.difficulty() != null) {
            sb.append("[面试设定] 你是一位专业技术面试官，请继续追问并保持面试节奏。\n");
            if (StringUtils.hasText(request.position())) {
                sb.append("岗位: ").append(request.position()).append("\n");
            }
            if (request.difficulty() != null) {
                sb.append("难度: L").append(request.difficulty()).append("\n");
            }
            sb.append("\n");
        }
        sb.append(request.message().trim());
        return sb.toString();
    }

    private List<ChatMessage> toChatContext(List<WsChatMessage> context) {
        if (context == null || context.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> result = new ArrayList<>(context.size());
        for (WsChatMessage item : context) {
            if (item == null || !StringUtils.hasText(item.content())) {
                continue;
            }
            String role = StringUtils.hasText(item.role()) ? item.role().trim() : "user";
            result.add(new ChatMessage(role, item.content().trim()));
        }
        return result;
    }

    private List<ChatMessage> trimContext(List<ChatMessage> context) {
        if (context == null || context.size() <= MAX_CONTEXT_MESSAGES) {
            return context;
        }
        return context.subList(context.size() - MAX_CONTEXT_MESSAGES, context.size());
    }

    private void sendEvent(WebSocketSession session, String type, String message, Map<String, Object> data) throws IOException {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", type);
        wrapper.put("message", message);
        wrapper.put("data", data == null ? Map.of() : data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wrapper)));
    }

    private void sendError(WebSocketSession session, String code, String message, Map<String, Object> extra) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        if (extra != null) {
            data.putAll(extra);
        }
        sendEvent(session, "error", message, data);
    }

    private String safeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "none";
        }
        return reason.trim().toLowerCase().replaceAll("[^a-z0-9._:-]", "_");
    }

    private record WsInterviewRequest(
            String message,
            String position,
            Integer difficulty,
            List<WsChatMessage> context
    ) {
    }

    private record WsChatMessage(String role, String content) {
    }
}
