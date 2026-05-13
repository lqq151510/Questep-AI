package com.interview.application.service;

import com.interview.application.dto.ChatMessage;
import com.interview.application.dto.ChatRequest;
import com.interview.application.dto.ChatResponse;
import com.interview.application.port.LlmGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatApplicationServiceTest {

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private MaterialRagApplicationService materialRagApplicationService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Test
    @DisplayName("chat should use template prompt and return llm reply")
    void chatShouldUseTemplatePromptAndReturnLlmReply() {
        ChatApplicationService service = new ChatApplicationService(
                llmGateway,
                materialRagApplicationService,
                promptTemplateService
        );
        ChatRequest request = new ChatRequest("解释 volatile", List.of(new ChatMessage("user", "先说可见性")));

        when(materialRagApplicationService.retrieveContext(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of("Java 内存模型"));
        when(promptTemplateService.resolveTemplate(anyString(), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn("template prompt");
        when(llmGateway.chat(1L, "template prompt")).thenReturn("volatile 保证可见性");

        ChatResponse response = service.chat(1L, request);

        assertEquals("volatile 保证可见性", response.reply());
        verify(materialRagApplicationService).retrieveContext(1L, "解释 volatile", 3);
        verify(promptTemplateService).resolveTemplate(org.mockito.ArgumentMatchers.eq("chat_default"), org.mockito.ArgumentMatchers.anyMap());
        verify(llmGateway).chat(1L, "template prompt");
    }

    @Test
    @DisplayName("chat should fallback to xml prompt when template fails")
    void chatShouldFallbackToXmlPromptWhenTemplateFails() {
        ChatApplicationService service = new ChatApplicationService(
                llmGateway,
                materialRagApplicationService,
                promptTemplateService
        );
        ChatRequest request = new ChatRequest("继续追问", List.of(new ChatMessage("assistant", "请介绍锁升级")));

        when(materialRagApplicationService.retrieveContext(anyLong(), anyString(), anyInt()))
                .thenReturn(List.of("锁膨胀", "偏向锁"));
        doThrow(new IllegalStateException("template missing"))
                .when(promptTemplateService)
                .resolveTemplate(anyString(), org.mockito.ArgumentMatchers.anyMap());
        when(llmGateway.chat(org.mockito.ArgumentMatchers.eq(2L), anyString())).thenReturn("fallback ok");

        ChatResponse response = service.chat(2L, request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmGateway).chat(org.mockito.ArgumentMatchers.eq(2L), promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("<conversation_history>"));
        assertTrue(prompt.contains("<retrieved_context>"));
        assertTrue(prompt.contains("<user_query>"));
        assertEquals("fallback ok", response.reply());
    }

    @Test
    @DisplayName("chatStream should stream tokens from gateway")
    void chatStreamShouldStreamTokensFromGateway() {
        ChatApplicationService service = new ChatApplicationService(
                llmGateway,
                materialRagApplicationService,
                promptTemplateService
        );
        ChatRequest request = new ChatRequest("模拟面试", List.of());
        when(materialRagApplicationService.retrieveContext(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(promptTemplateService.resolveTemplate(anyString(), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn("stream prompt");

        AtomicReference<String> collected = new AtomicReference<>("");
        org.mockito.Mockito.doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("A");
            consumer.accept("I");
            return null;
        }).when(llmGateway).chatStream(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq("stream prompt"), org.mockito.ArgumentMatchers.any());

        service.chatStream(3L, request, token -> collected.set(collected.get() + token));

        assertEquals("AI", collected.get());
        verify(llmGateway).chatStream(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq("stream prompt"), org.mockito.ArgumentMatchers.any());
    }
}
