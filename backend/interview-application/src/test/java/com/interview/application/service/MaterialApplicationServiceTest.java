package com.interview.application.service;

import com.interview.application.dto.UploadMaterialResult;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialApplicationServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private AsyncTaskRecordRepository asyncTaskRecordRepository;

    @InjectMocks
    private MaterialApplicationService materialApplicationService;

    private Material testMaterial;
    private AsyncTaskRecord testTask;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testMaterial = new Material(
                1L,
                testUserId,
                "test.pdf",
                "application/pdf",
                "UPLOAD",
                "/storage/test.pdf",
                null,
                "PENDING",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        testTask = new AsyncTaskRecord(
                1L,
                "PARSE-123456",
                "MATERIAL_PARSE",
                1L,
                "PENDING",
                0,
                null,
                testUserId,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Test successful material upload and task creation")
    void testUploadAndCreateParseTaskSuccess() {
        String name = "test-document.pdf";
        String fileType = "application/pdf";
        String storagePath = "/storage/uploads/test-document.pdf";

        when(materialRepository.save(testUserId, name, fileType, storagePath)).thenReturn(testMaterial);
        when(asyncTaskRecordRepository.create(
                anyString(),
                eq("MATERIAL_PARSE"),
                eq(testMaterial.id()),
                eq(testUserId)
        )).thenReturn(testTask);

        UploadMaterialResult result = materialApplicationService.uploadAndCreateParseTask(
                testUserId, name, fileType, storagePath
        );

        assertNotNull(result);
        assertNotNull(result.material());
        assertNotNull(result.task());
        assertEquals(testMaterial.id(), result.material().id());
        assertEquals(testTask.id(), result.task().id());
        verify(materialRepository, times(1)).save(testUserId, name, fileType, storagePath);
        verify(asyncTaskRecordRepository, times(1)).create(
                anyString(), eq("MATERIAL_PARSE"), eq(testMaterial.id()), eq(testUserId)
        );
    }

    @Test
    @DisplayName("Test list materials returns user's materials")
    void testListMaterials() {
        Material material2 = new Material(
                2L,
                testUserId,
                "notes.docx",
                "application/docx",
                "UPLOAD",
                "/storage/notes.docx",
                null,
                "READY",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        List<Material> expectedMaterials = Arrays.asList(testMaterial, material2);

        when(materialRepository.findByUserId(testUserId)).thenReturn(expectedMaterials);

        List<Material> result = materialApplicationService.list(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedMaterials, result);
        verify(materialRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("Test list materials returns empty list when user has no materials")
    void testListMaterialsEmpty() {
        when(materialRepository.findByUserId(testUserId)).thenReturn(List.of());

        List<Material> result = materialApplicationService.list(testUserId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
