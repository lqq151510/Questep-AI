package com.interview.api.controller;

import com.interview.api.security.JwtAuthenticationFilter;
import com.interview.application.dto.ChatRequest;
import com.interview.application.dto.ChatResponse;
import com.interview.application.service.ChatApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatApplicationService chatApplicationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("chat should return 401 without security context")
    void chatShouldReturn401WithoutSecurityContext() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello","context":[]}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("chat should return reply for authenticated user")
    void chatShouldReturnReply() throws Exception {
        mockUser(3L);
        when(chatApplicationService.chat(eq(3L), org.mockito.ArgumentMatchers.any(ChatRequest.class)))
                .thenReturn(new ChatResponse("你好，这里是 AI 面试官"));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"请继续追问","context":[{"role":"assistant","content":"上题回答不错"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("你好，这里是 AI 面试官"));

        verify(chatApplicationService).chat(eq(3L), org.mockito.ArgumentMatchers.any(ChatRequest.class));
    }

    @Test
    @DisplayName("chat should validate blank message")
    void chatShouldValidateBlankMessage() throws Exception {
        mockUser(3L);
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"","context":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void mockUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, createAuthorityList("ROLE_USER"))
        );
    }
}
