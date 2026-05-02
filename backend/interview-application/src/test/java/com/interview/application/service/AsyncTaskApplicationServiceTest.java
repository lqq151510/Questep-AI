package com.interview.application.service;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncTaskApplicationServiceTest {

    @Mock
    private AsyncTaskRecordRepository asyncTaskRecordRepository;

    @InjectMocks
    private AsyncTaskApplicationService asyncTaskApplicationService;

    private AsyncTaskRecord testTask;

    @BeforeEach
    void setUp() {
        testTask = new AsyncTaskRecord(
                1L,
                "PARSE-123456",
                "MATERIAL_PARSE",
                100L,
                "PROCESSING",
                50,
                null,
                1L,
                LocalDateTime.now(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Test get task by taskNo successfully")
    void testGetByTaskNoSuccess() {
        when(asyncTaskRecordRepository.findByTaskNo("PARSE-123456")).thenReturn(Optional.of(testTask));

        AsyncTaskRecord result = asyncTaskApplicationService.getByTaskNo("PARSE-123456");

        assertNotNull(result);
        assertEquals("PARSE-123456", result.taskNo());
        assertEquals("MATERIAL_PARSE", result.taskType());
        verify(asyncTaskRecordRepository, times(1)).findByTaskNo("PARSE-123456");
    }

    @Test
    @DisplayName("Test get task by taskNo throws exception when not found")
    void testGetByTaskNoNotFound() {
        when(asyncTaskRecordRepository.findByTaskNo("NONEXISTENT")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> asyncTaskApplicationService.getByTaskNo("NONEXISTENT")
        );

        assertEquals("Task not found", exception.getMessage());
    }
}
