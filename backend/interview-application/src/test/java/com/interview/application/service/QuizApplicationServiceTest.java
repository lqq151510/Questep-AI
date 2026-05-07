package com.interview.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.application.port.LlmGateway;
import com.interview.application.dto.GenerateQuizCommand;
import com.interview.application.dto.GeneratedQuizResult;
import com.interview.application.service.quiz.QuizFallbackQuestionFactory;
import com.interview.application.service.quiz.QuizGenerationPolicy;
import com.interview.application.service.quiz.QuizPromptBuilder;
import com.interview.application.service.quiz.StructuredQuizPayloadParser;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.Material;
import com.interview.domain.model.Question;
import com.interview.domain.repository.MaterialRepository;
import com.interview.domain.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizApplicationServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private TransactionTemplate txTemplate;

    private QuizApplicationService quizApplicationService;

    private Material material;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        when(txTemplate.execute(org.mockito.ArgumentMatchers.any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<List<Question>> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        quizApplicationService = new QuizApplicationService(
                materialRepository,
                questionRepository,
                llmGateway,
                new QuizGenerationPolicy(),
                new QuizPromptBuilder("Keep each question grounded in backend project experience."),
                new StructuredQuizPayloadParser(new ObjectMapper()),
                new QuizFallbackQuestionFactory(),
                txTemplate
        );
        material = new Material(
                100L,
                1L,
                "Java并发实战笔记",
                "TXT",
                "UPLOAD",
                "/tmp/material.txt",
                null,
                "SUCCESS",
                null,
                null,
                now,
                now,
                now
        );

        AtomicLong questionId = new AtomicLong(1);
        when(questionRepository.save(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), eq(3), eq(TaskConstants.SOURCE_TYPE_AI), anyString()))
                .thenAnswer(invocation -> new Question(
                        questionId.getAndIncrement(),
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4),
                        invocation.getArgument(5),
                        invocation.getArgument(6),
                        invocation.getArgument(7),
                        invocation.getArgument(8),
                        now,
                        now
                ));
    }

    @Test
    @DisplayName("should use structured llm output when schema is valid")
    void shouldUseStructuredLlmOutputWhenSchemaIsValid() {
        GenerateQuizCommand command = new GenerateQuizCommand(List.of(100L), "short", 3, 2, false);
        when(materialRepository.findByUserIdAndIds(1L, List.of(100L))).thenReturn(List.of(material));
        when(llmGateway.chat(anyString())).thenReturn("""
                {"summary":"覆盖并发容器与可见性边界","questions":[
                  {"stem":"解释volatile的可见性语义","referenceAnswer":"可见性+禁止重排序","analysis":"重点在happens-before"},
                  {"stem":"ConcurrentHashMap为什么比Hashtable吞吐更高","referenceAnswer":"分段/桶级并发+CAS","analysis":"锁粒度更细"}
                ]}
                """);

        GeneratedQuizResult result = quizApplicationService.generate(1L, command);

        assertEquals("覆盖并发容器与可见性边界", result.modelBrief());
        assertEquals(2, result.questions().size());

        ArgumentCaptor<String> stemCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> modelNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(questionRepository, times(2)).save(
                eq(100L), eq(1L), eq("SHORT_ANSWER"),
                stemCaptor.capture(), anyString(), anyString(), eq(3), eq(TaskConstants.SOURCE_TYPE_AI), modelNameCaptor.capture()
        );
        List<String> stems = stemCaptor.getAllValues();
        assertTrue(stems.get(0).startsWith("1."));
        assertTrue(stems.get(1).startsWith("2."));
        assertEquals(List.of("llm-structured", "llm-structured"), modelNameCaptor.getAllValues());
    }

    @Test
    @DisplayName("should fallback to deterministic drafts when llm schema is invalid")
    void shouldFallbackToDeterministicDraftsWhenSchemaIsInvalid() {
        GenerateQuizCommand command = new GenerateQuizCommand(List.of(100L), "short", 3, 1, true);
        when(materialRepository.findByUserIdAndIds(1L, List.of(100L))).thenReturn(List.of(material));
        when(llmGateway.chat(anyString())).thenReturn("this-is-not-json");

        GeneratedQuizResult result = quizApplicationService.generate(1L, command);

        assertTrue(result.modelBrief().contains("fallback"));
        assertEquals(1, result.questions().size());

        ArgumentCaptor<String> stemCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> modelNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(questionRepository, times(1)).save(
                eq(100L), eq(1L), eq("SHORT_ANSWER"),
                stemCaptor.capture(), anyString(), anyString(), eq(3), eq(TaskConstants.SOURCE_TYPE_AI), modelNameCaptor.capture()
        );
        assertTrue(stemCaptor.getValue().startsWith("1."));
        assertEquals("fallback-local", modelNameCaptor.getValue());
    }
}
