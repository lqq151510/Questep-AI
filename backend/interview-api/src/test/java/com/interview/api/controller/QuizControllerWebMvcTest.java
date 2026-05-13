package com.interview.api.controller;

import com.interview.api.config.ApiExceptionHandler;
import com.interview.application.dto.GenerateQuizCommand;
import com.interview.application.dto.GeneratedQuizResult;
import com.interview.application.service.QuizApplicationService;
import com.interview.domain.model.Question;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizControllerWebMvcTest {

    @Mock
    private QuizApplicationService quizApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new QuizController(quizApplicationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("generate should return quiz result")
    void generateShouldReturnQuizResult() throws Exception {
        mockUser(2L);
        GeneratedQuizResult result = new GeneratedQuizResult(
                "trace-1",
                "模型摘要",
                List.of(question(1L, "解释 Reactor 背压")),
                false,
                0,
                List.of()
        );
        when(quizApplicationService.generate(eq(2L), org.mockito.ArgumentMatchers.any(GenerateQuizCommand.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/quizzes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"materialIds":[1],"questionType":"short","difficulty":3,"count":1,"interviewMode":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questions[0].stemText").value("解释 Reactor 背压"));
    }

    @Test
    @DisplayName("recent should reject invalid paging")
    void recentShouldRejectInvalidPaging() throws Exception {
        mockUser(2L);

        mockMvc.perform(get("/api/v1/quizzes/questions?page=-1&pageSize=20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("recent should call service when paging is valid")
    void recentShouldCallService() throws Exception {
        mockUser(5L);
        when(quizApplicationService.recent(eq(5L), anyInt(), anyInt()))
                .thenReturn(List.of(question(11L, "什么是 JVM GC")));

        mockMvc.perform(get("/api/v1/quizzes/questions?page=0&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(11L));

        verify(quizApplicationService).recent(5L, 0, 20);
    }

    private void mockUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, createAuthorityList("ROLE_USER"))
        );
    }

    private Question question(Long id, String stem) {
        LocalDateTime now = LocalDateTime.now();
        return new Question(
                id,
                1L,
                1L,
                "SHORT_ANSWER",
                stem,
                "answer",
                "analysis",
                3,
                "AI",
                "model",
                null,
                null,
                now,
                null,
                now.plusDays(1),
                "APPROVED",
                now,
                now
        );
    }
}
