package com.interview.application.service;

import com.interview.application.dto.ChatMessage;
import com.interview.application.dto.ChatRequest;
import com.interview.application.dto.ChatResponse;
import com.interview.application.port.LlmGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatApplicationService {

    private final LlmGateway llmGateway;

    public ChatApplicationService(LlmGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public ChatResponse chat(Long userId, ChatRequest request) {
        String prompt = buildPrompt(request.message(), request.context());
        String reply = llmGateway.chat(userId, prompt);
        return new ChatResponse(reply);
    }

    private String buildPrompt(String message, List<ChatMessage> context) {
        if (context == null || context.isEmpty()) {
            return message;
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : context) {
            String roleLabel = "user".equalsIgnoreCase(msg.role()) ? "用户" : "AI";
            sb.append(roleLabel).append("：").append(msg.content()).append("\n");
        }
        sb.append("用户：").append(message);
        return sb.toString();
    }
}
