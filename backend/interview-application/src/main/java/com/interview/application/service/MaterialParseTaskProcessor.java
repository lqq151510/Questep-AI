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

import java.io.IOException;
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

            // Categorize the error
            String errorCode;
            String stage;
            boolean retryable;

            if (e instanceof ResourceNotFoundException) {
                errorCode = "MATERIAL_NOT_FOUND";
                stage = "PARSE";
                retryable = false;
            } else if (e instanceof IllegalArgumentException) {
                errorCode = "BAD_REQUEST";
                stage = "PARSE";
                retryable = false;
            } else if (e instanceof IllegalStateException) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("LLM returned empty")) {
                    errorCode = "LLM_EMPTY_RESPONSE";
                    stage = "LLM";
                    retryable = true;
                } else if (msg.contains("empty") || msg.contains("does not exist")) {
                    errorCode = "FILE_ERROR";
                    stage = "STORAGE";
                    retryable = false;
                } else {
                    errorCode = "INTERNAL_ERROR";
                    stage = "PARSE";
                    retryable = true;
                }
            } else if (e instanceof IOException) {
                errorCode = "STORAGE_ERROR";
                stage = "STORAGE";
                retryable = true;
            } else {
                errorCode = "INTERNAL_ERROR";
                stage = "PARSE";
                retryable = true;
            }

            // Clean up temp files on non-retryable permanent errors
            if (!retryable) {
                try {
                    Material material = materialRepository.findById(task.bizId()).orElse(null);
                    if (material != null && material.storageUrl() != null) {
                        Path storagePath = Paths.get(material.storageUrl()).normalize();
                        if (Files.exists(storagePath)) {
                            Files.deleteIfExists(storagePath);
                            logger.info("Deleted temp file for non-retryable error: {}", storagePath);
                        }
                    }
                } catch (Exception cleanupError) {
                    logger.warn("Failed to clean up temp file for task {}: {}", task.taskNo(), cleanupError.getMessage());
                }
            }

            try {
                materialRepository.markParseFailure(task.bizId(), errorMsg);
            } catch (Exception updateMaterialError) {
                logger.error("Failed to update material parse failure state: materialId={}", task.bizId(), updateMaterialError);
            }
            asyncTaskRecordRepository.updateError(task.id(), errorMsg, errorCode, stage, retryable);
        }
    }

    private void parseMaterial(AsyncTaskRecord task) {
        logger.info("Parsing material with id: {}", task.bizId());

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 30);

        Material material = materialRepository.findById(task.bizId())
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + task.bizId()));

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 50);

        // Handle non-text file types (binary files that need special parsers)
        String fileType = material.fileType();
        if (fileType != null && isNonTextFileType(fileType)) {
            handleNonTextFile(material, fileType);
            return;
        }

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
        String analysis = llmGateway.chat(task.createdBy(), prompt);
        if (analysis == null || analysis.isBlank()) {
            throw new IllegalStateException("LLM returned empty analysis for material: " + material.id());
        }

        materialRepository.markParseSuccess(material.id(), sha256Hex(content), analysis);

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 80);
        logger.info("Material parsing completed for id: {}", task.bizId());
    }

    private boolean isNonTextFileType(String fileType) {
        return "PDF".equals(fileType) || "DOCX".equals(fileType)
                || "PNG".equals(fileType) || "JPG".equals(fileType) || "JPEG".equals(fileType);
    }

    private void handleNonTextFile(Material material, String fileType) {
        String analysis;
        switch (fileType) {
            case "PDF":
                analysis = "PDF 文件解析器暂未配置，文件已保存等待后续处理";
                break;
            case "DOCX":
                analysis = "DOCX 文件解析器暂未配置，文件已保存等待后续处理";
                break;
            case "PNG":
            case "JPG":
            case "JPEG":
                analysis = "图片文件已上传 (OCR 暂未接入)，文件名: " + material.name();
                break;
            default:
                analysis = "文件类型 " + fileType + " 的解析器暂未配置，文件已保存等待后续处理";
        }
        logger.warn("Non-text file type {} for material {}, storing placeholder analysis", fileType, material.id());
        materialRepository.markParseSuccess(material.id(), null, analysis);
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
