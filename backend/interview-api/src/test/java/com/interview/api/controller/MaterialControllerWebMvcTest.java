package com.interview.api.controller;

import com.interview.api.config.ApiExceptionHandler;
import com.interview.application.dto.UploadMaterialResult;
import com.interview.application.service.MaterialApplicationService;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaterialControllerWebMvcTest {

    @Mock
    private MaterialApplicationService materialApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MaterialController controller = new MaterialController(materialApplicationService);
        ReflectionTestUtils.setField(controller, "baseDir", System.getProperty("java.io.tmpdir") + "/interview-material-test");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("upload should return 200 for authenticated user")
    void uploadShouldReturn200() throws Exception {
        mockUser(1L);
        UploadMaterialResult result = new UploadMaterialResult(
                material(100L, "notes.md"),
                new AsyncTaskRecord(1L, "PARSE-001", "MATERIAL_PARSE", "MATERIAL", 100L, "PENDING", 0, null, 1L, null, null, LocalDateTime.now(), LocalDateTime.now(), null, null, null)
        );
        when(materialApplicationService.uploadAndCreateParseTask(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                MediaType.TEXT_PLAIN_VALUE,
                "# hello".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/materials/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.material.id").value(100L))
                .andExpect(jsonPath("$.data.task.taskNo").value("PARSE-001"));

        verify(materialApplicationService).uploadAndCreateParseTask(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("list should return 401 when user context is missing")
    void listShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("delete should call service for authenticated user")
    void deleteShouldCallService() throws Exception {
        mockUser(8L);

        mockMvc.perform(delete("/api/v1/materials/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(materialApplicationService).delete(8L, 123L);
    }

    @Test
    @DisplayName("upload should reject unsupported extension")
    void uploadShouldRejectUnsupportedExtension() throws Exception {
        mockUser(1L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "abc".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/materials/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void mockUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, createAuthorityList("ROLE_USER"))
        );
    }

    private Material material(Long id, String name) {
        LocalDateTime now = LocalDateTime.now();
        return new Material(
                id,
                1L,
                name,
                "MD",
                "UPLOAD",
                "/tmp/" + name,
                null,
                "PENDING",
                null,
                null,
                null,
                now,
                now
        );
    }
}
