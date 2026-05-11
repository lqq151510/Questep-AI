package com.interview.application.service;

import com.interview.application.dto.ChatMessage;
import com.interview.application.dto.ChatRequest;
import com.interview.application.dto.ChatResponse;
import com.interview.application.port.LlmGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatApplicationService {

    private static final int RAG_TOP_K = 3;

    private final LlmGateway llmGateway;
    private final MaterialRagApplicationService materialRagApplicationService;

    public ChatApplicationService(
            LlmGateway llmGateway,
            MaterialRagApplicationService materialRagApplicationService
    ) {
        this.llmGateway = llmGateway;
        this.materialRagApplicationService = materialRagApplicationService;
    }

    public ChatResponse chat(Long userId, ChatRequest request) {
        List<String> retrievedContexts = materialRagApplicationService.retrieveContext(userId, request.message(), RAG_TOP_K);
        String prompt = buildPrompt(request.message(), request.context(), retrievedContexts);
        String reply = llmGateway.chat(userId, prompt);
        return new ChatResponse(reply);
    }

    private String buildPrompt(String message, List<ChatMessage> context, List<String> retrievedContexts) {
        StringBuilder sb = new StringBuilder();
        if (retrievedContexts != null && !retrievedContexts.isEmpty()) {
            sb.append("<retrieved_context>\n");
            for (int i = 0; i < retrievedContexts.size(); i++) {
                sb.append("  <chunk index=\"").append(i + 1).append("\">")
                        .append(sanitize(retrievedContexts.get(i)))
                        .append("</chunk>\n");
            }
            sb.append("</retrieved_context>\n\n");
        }
        if (context == null || context.isEmpty()) {
            sb.append("<user_query>\n").append(sanitize(message)).append("\n</user_query>");
            return sb.toString();
        }
        sb.append("<conversation_history>\n");
        for (ChatMessage msg : context) {
            String roleLabel = "user".equalsIgnoreCase(msg.role()) ? "user" : "assistant";
            sb.append("  <").append(roleLabel).append(">\n");
            sb.append("    ").append(sanitize(msg.content())).append("\n");
            sb.append("  </").append(roleLabel).append(">\n");
        }
        sb.append("</conversation_history>\n\n");
        sb.append("<user_query>\n").append(sanitize(message)).append("\n</user_query>");
        return sb.toString();
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.replace("</", "< /");
    }
}
