package com.interview.application.service;

import com.interview.aigateway.client.LlmGateway;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

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

            if (TaskConstants.TYPE_MATERIAL_PARSE.equals(task.taskType())) {
                parseMaterial(task);
            }

            asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_SUCCESS, 100);
            logger.info("Task completed successfully: taskNo={}", task.taskNo());

        } catch (Exception e) {
            logger.error("Error processing task: taskNo={}, error={}", task.taskNo(), e.getMessage(), e);
            asyncTaskRecordRepository.updateError(task.id(), e.getMessage());
        }
    }

    private void parseMaterial(AsyncTaskRecord task) {
        logger.info("Parsing material with id: {}", task.bizId());

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 30);

        Material material = materialRepository.findById(task.bizId())
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + task.bizId()));

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 50);

        String storageUrl = material.storageUrl();
        if (storageUrl != null && Files.exists(Paths.get(storageUrl))) {
            try {
                String content = Files.readString(Paths.get(storageUrl));
                String prompt = "Please analyze this learning material and summarize key points: " +
                        "Material name: " + material.name() + "\n" +
                        "Content: " + (content.length() > 1000 ? content.substring(0, 1000) : content);

                logger.info("Calling LLM to analyze material: {}", material.name());
                llmGateway.chat(prompt);

            } catch (Exception e) {
                logger.warn("Failed to read or analyze material content: {}", e.getMessage());
            }
        }

        asyncTaskRecordRepository.updateStatus(task.id(), TaskConstants.STATUS_PROCESSING, 80);
        logger.info("Material parsing completed for id: {}", task.bizId());
    }
}
