package com.interview.application.service;

import com.interview.application.dto.UploadMaterialResult;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MaterialApplicationService {

    private final MaterialRepository materialRepository;
    private final AsyncTaskRecordRepository asyncTaskRecordRepository;

    public MaterialApplicationService(MaterialRepository materialRepository, AsyncTaskRecordRepository asyncTaskRecordRepository) {
        this.materialRepository = materialRepository;
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
    }

    @Transactional
    public UploadMaterialResult uploadAndCreateParseTask(Long userId, String name, String fileType, String storagePath) {
        Material material = materialRepository.save(userId, name, fileType, storagePath);
        AsyncTaskRecord task = asyncTaskRecordRepository.create(
                "PARSE-" + UUID.randomUUID().toString().replace("-", ""),
                "MATERIAL_PARSE",
                material.id(),
                userId
        );
        return new UploadMaterialResult(material, task);
    }

    public List<Material> list(Long userId) {
        return materialRepository.findByUserId(userId);
    }
}
