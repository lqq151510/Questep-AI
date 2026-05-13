package com.interview.api.controller;

import com.interview.application.service.TokenBlacklistService;
import com.interview.application.service.LoginAttemptService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = com.interview.api.InterviewApiApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:api_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema.sql",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "app.jwt.secret=test-secret-key-0123456789-0123456789",
                "management.endpoints.web.exposure.include=health",
                "springdoc.api-docs.enabled=false"
        }
)
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService.recordSuccessfulAttempt("demo_user");
    }

    @Test
    @DisplayName("login with valid credentials returns token")
    void loginWithValidCredentialsReturnsToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo_user","password":"demo123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.data.token");
        String refreshToken = JsonPath.read(response, "$.data.refreshToken");

        assertNotNull(token);
        assertTrue(token.length() > 10);
        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 10);
    }

    @Test
    @DisplayName("login with wrong password returns 401")
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo_user","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("login with non-existent user returns 401")
    void loginWithNonExistentUserReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"no_one","password":"whatever"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("protected endpoint returns 403 without token (Spring Security default)")
    void protectedEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("protected endpoint returns 200 with valid token")
    void protectedEndpointReturns200WithValidToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo_user","password":"demo123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(get("/api/v1/materials")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("login with invalid request body returns 400")
    void loginWithInvalidRequestBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("health endpoint is publicly accessible")
    void healthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login blocks after repeated failures and returns 401")
    void loginBlocksAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"demo_user","password":"wrong"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        MvcResult blocked = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo_user","password":"demo123456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String message = JsonPath.read(blocked.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.message");
        assertNotNull(message);
        assertTrue(message.contains("锁定") || message.contains("分钟"),
                "Expected lockout message containing 锁定 or 分钟 but got: " + message);
    }
}
