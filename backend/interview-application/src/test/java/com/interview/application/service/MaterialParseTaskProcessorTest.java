package com.interview.application.service;

import com.interview.application.port.LlmGateway;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialParseTaskProcessorTest {

    @Mock
    private AsyncTaskRecordRepository asyncTaskRecordRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private LlmGateway llmGateway;

    @InjectMocks
    private MaterialParseTaskProcessor materialParseTaskProcessor;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should mark task and material as success after parse finishes")
    void shouldMarkSuccessAfterParse() throws Exception {
        Path materialPath = tempDir.resolve("sample.txt");
        Files.writeString(materialPath, "Spring Boot + MyBatis integration notes");

        AsyncTaskRecord task = buildTask(10L, 100L);
        Material material = buildMaterial(100L, materialPath.toString());
        when(materialRepository.findById(100L)).thenReturn(Optional.of(material));
        when(llmGateway.chat(anyString())).thenReturn("summary");

        materialParseTaskProcessor.processTask(task);

        verify(materialRepository).markParseSuccess(eq(100L), anyString(), eq("summary"));
        verify(materialRepository, never()).markParseFailure(anyLong(), anyString());
        verify(asyncTaskRecordRepository).updateStatus(10L, TaskConstants.STATUS_SUCCESS, 100);
        verify(asyncTaskRecordRepository, never()).updateError(eq(10L), anyString());
    }

    @Test
    @DisplayName("should mark task and material as failed when llm fails")
    void shouldMarkFailureWhenLlmFails() throws Exception {
        Path materialPath = tempDir.resolve("sample.txt");
        Files.writeString(materialPath, "failure case");

        AsyncTaskRecord task = buildTask(11L, 101L);
        Material material = buildMaterial(101L, materialPath.toString());
        when(materialRepository.findById(101L)).thenReturn(Optional.of(material));
        when(llmGateway.chat(anyString())).thenThrow(new RuntimeException("OpenAI gateway timeout"));

        materialParseTaskProcessor.processTask(task);

        verify(materialRepository).markParseFailure(eq(101L), contains("OpenAI gateway timeout"));
        verify(materialRepository, never()).markParseSuccess(eq(101L), anyString(), anyString());
        verify(asyncTaskRecordRepository).updateError(eq(11L), contains("OpenAI gateway timeout"));
        verify(asyncTaskRecordRepository, never()).updateStatus(11L, TaskConstants.STATUS_SUCCESS, 100);
    }

    private AsyncTaskRecord buildTask(Long id, Long bizId) {
        LocalDateTime now = LocalDateTime.now();
        return new AsyncTaskRecord(
                id,
                "PARSE-" + id,
                TaskConstants.TYPE_MATERIAL_PARSE,
                bizId,
                TaskConstants.STATUS_PENDING,
                0,
                null,
                1L,
                null,
                null,
                now,
                now
        );
    }

    private Material buildMaterial(Long id, String storagePath) {
        LocalDateTime now = LocalDateTime.now();
        return new Material(
                id,
                1L,
                "sample.txt",
                "TXT",
                "UPLOAD",
                storagePath,
                null,
                TaskConstants.STATUS_PENDING,
                null,
                null,
                null,
                now,
                now
        );
    }
}
