package com.interview.application.service;

import com.interview.application.port.LlmGateway;
import com.interview.common.constant.TaskConstants;
import com.interview.common.exception.ResourceNotFoundException;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class MaterialParseTaskProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MaterialParseTaskProcessor.class);

    private final AsyncTaskRecordRepository asyncTaskRecordRepository;
    private final MaterialRepository materialRepository;
    private final LlmGateway llmGateway;

    public MaterialParseTaskProcessor(
            AsyncTaskRecordRepository asyncTaskRecordRepository,
            MaterialRepository materialRepository,
            LlmGateway llmGateway
    ) {
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
        this.materialRepository = materialRepository;
        this.llmGateway = llmGateway;
    }

    public void processTask(AsyncTaskRecord task) {
        try {
            logger.info("Starting to process task: taskNo={}, taskType={}, bizId={}",
                    task.taskNo(), task.taskType(), task.bizId());

            asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 10);

            if (!TaskConstants.TYPE_MATERIAL_PARSE.equals(task.taskType())) {
                throw new IllegalArgumentException("Unsupported task type: " + task.taskType());
            }
            parseMaterial(task);

            asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_SUCCESS, 100);
            logger.info("Task completed successfully: taskNo={}", task.taskNo());

        } catch (Exception e) {
            logger.error("Error processing task: taskNo={}, error={}", task.taskNo(), e.getMessage(), e);
            String errorMsg = summarizeError(e);
            try {
                materialRepository.markParseFailure(task.bizId(), errorMsg);
            } catch (Exception updateMaterialError) {
                logger.error("Failed to update material parse failure state: materialId={}", task.bizId(), updateMaterialError);
            }
            asyncTaskRecordRepository.updateError(task.id(), errorMsg);
        }
    }

    private void parseMaterial(AsyncTaskRecord task) {
        logger.info("Parsing material with id: {}", task.bizId());

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 30);

        Material material = materialRepository.findById(task.bizId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + task.bizId()));

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 50);

        String storageUrl = material.storageUrl();
        if (storageUrl == null || storageUrl.isBlank()) {
            throw new IllegalStateException("Material storage path is empty: materialId=" + material.id());
        }
        Path storagePath = Paths.get(storageUrl).normalize();
        if (!Files.exists(storagePath) || !Files.isRegularFile(storagePath)) {
            throw new IllegalStateException("Material file does not exist: " + storagePath);
        }

        String content;
        try {
            content = Files.readString(storagePath);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read material file: " + storagePath, ex);
        }
        if (content.isBlank()) {
            throw new IllegalStateException("Material content is empty: materialId=" + material.id());
        }

        String prompt = "Please analyze this learning material and summarize key points: " +
                "Material name: " + material.name() + "\n" +
                "Content: " + (content.length() > 2000 ? content.substring(0, 2000) : content);
        logger.info("Calling LLM to analyze material: {}", material.name());
        String analysis = llmGateway.chat(prompt);
        if (analysis == null || analysis.isBlank()) {
            throw new IllegalStateException("LLM returned empty analysis for material: " + material.id());
        }

        materialRepository.markParseSuccess(material.id(), sha256Hex(content), analysis);

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 80);
        logger.info("Material parsing completed for id: {}", task.bizId());
    }

    private String summarizeError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
