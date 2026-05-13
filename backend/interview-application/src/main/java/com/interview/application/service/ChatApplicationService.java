package com.interview.application.service;

import com.interview.application.dto.ChatMessage;
import com.interview.application.dto.ChatRequest;
import com.interview.application.dto.ChatResponse;
import com.interview.application.port.LlmGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class ChatApplicationService {

    private static final int RAG_TOP_K = 3;

    private final LlmGateway llmGateway;
    private final MaterialRagApplicationService materialRagApplicationService;
    private final PromptTemplateService promptTemplateService;

    public ChatApplicationService(
            LlmGateway llmGateway,
            MaterialRagApplicationService materialRagApplicationService,
            PromptTemplateService promptTemplateService
    ) {
        this.llmGateway = llmGateway;
        this.materialRagApplicationService = materialRagApplicationService;
        this.promptTemplateService = promptTemplateService;
    }

    public ChatResponse chat(Long userId, ChatRequest request) {
        List<String> retrievedContexts = materialRagApplicationService.retrieveContext(userId, request.message(), RAG_TOP_K);
        String prompt = buildPrompt(request.message(), request.context(), retrievedContexts);
        String reply = llmGateway.chat(userId, prompt);
        return new ChatResponse(reply);
    }

    public void chatStream(Long userId, ChatRequest request, Consumer<String> tokenConsumer) {
        List<String> retrievedContexts = materialRagApplicationService.retrieveContext(userId, request.message(), RAG_TOP_K);
        String prompt = buildPrompt(request.message(), request.context(), retrievedContexts);
        llmGateway.chatStream(userId, prompt, tokenConsumer);
    }

    private String buildPrompt(String message, List<ChatMessage> context, List<String> retrievedContexts) {
        String fromTemplate = buildPromptFromTemplate(message, context, retrievedContexts);
        if (fromTemplate != null) {
            return fromTemplate;
        }
        return buildPromptFallback(message, context, retrievedContexts);
    }

    private String buildPromptFromTemplate(String message, List<ChatMessage> context, List<String> retrievedContexts) {
        try {
            boolean hasRetrieved = retrievedContexts != null && !retrievedContexts.isEmpty();
            boolean hasHistory = context != null && !context.isEmpty();

            java.util.Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("message", sanitize(message));
            vars.put("hasHistory", hasHistory);
            vars.put("hasRetrieved", hasRetrieved);

            if (hasRetrieved) {
                java.util.List<java.util.Map<String, Object>> chunks = new java.util.ArrayList<>();
                for (int i = 0; i < retrievedContexts.size(); i++) {
                    java.util.Map<String, Object> chunk = new java.util.HashMap<>();
                    chunk.put("index", String.valueOf(i + 1));
                    chunk.put("text", sanitize(retrievedContexts.get(i)));
                    chunks.add(chunk);
                }
                vars.put("retrievedContexts", chunks);
            }

            if (hasHistory) {
                java.util.List<java.util.Map<String, Object>> history = new java.util.ArrayList<>();
                for (ChatMessage msg : context) {
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("role", "user".equalsIgnoreCase(msg.role()) ? "user" : "assistant");
                    item.put("content", sanitize(msg.content()));
                    history.add(item);
                }
                vars.put("history", history);
            }

            return promptTemplateService.resolveTemplate("chat_default", vars);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPromptFallback(String message, List<ChatMessage> context, List<String> retrievedContexts) {
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
