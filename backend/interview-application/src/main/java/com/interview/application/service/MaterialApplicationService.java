package com.interview.application.service;

import com.interview.application.dto.UploadMaterialResult;
import com.interview.common.constant.TaskConstants;
import com.interview.common.exception.ResourceNotFoundException;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class MaterialApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(MaterialApplicationService.class);

    private final MaterialRepository materialRepository;
    private final AsyncTaskRecordRepository asyncTaskRecordRepository;

    @Value("${app.storage.material-dir:./data/materials}")
    private String baseDir;

    public MaterialApplicationService(MaterialRepository materialRepository, AsyncTaskRecordRepository asyncTaskRecordRepository) {
        this.materialRepository = materialRepository;
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
    }

    @Transactional
    public UploadMaterialResult uploadAndCreateParseTask(Long userId, String name, String fileType, String storagePath) {
        Material material = materialRepository.save(userId, name, fileType, storagePath);
        AsyncTaskRecord task = asyncTaskRecordRepository.create(
                TaskConstants.TASK_NO_PREFIX + UUID.randomUUID().toString().replace("-", ""),
                TaskConstants.TYPE_MATERIAL_PARSE,
                TaskConstants.BIZ_TYPE_MATERIAL_PARSE,
                material.id(),
                userId
        );
        return new UploadMaterialResult(material, task);
    }

    @Transactional
    public UploadMaterialResult retryParseTask(Long userId, Long materialId) {
        Material material = materialRepository.findByIdAndUserId(materialId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found or not owned by user: " + materialId));
        materialRepository.markParsePending(materialId);
        AsyncTaskRecord task = asyncTaskRecordRepository.create(
                TaskConstants.TASK_NO_PREFIX + UUID.randomUUID().toString().replace("-", ""),
                TaskConstants.TYPE_MATERIAL_PARSE,
                TaskConstants.BIZ_TYPE_MATERIAL_PARSE,
                material.id(),
                userId
        );
        return new UploadMaterialResult(material, task);
    }

    public List<Material> list(Long userId) {
        return materialRepository.findByUserId(userId);
    }

    @Transactional
    public void delete(Long userId, Long materialId) {
        Material material = materialRepository.findByIdAndUserId(materialId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found or not owned by user: " + materialId));
        deleteStorageFile(material.storageUrl());
        materialRepository.deleteById(materialId);
    }

    private void deleteStorageFile(String storageUrl) {
        if (storageUrl == null || storageUrl.isBlank()) return;
        try {
            Path filePath = Path.of(storageUrl).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.warn("Failed to delete storage file: {} - {}", storageUrl, e.getMessage());
        }
    }
}
