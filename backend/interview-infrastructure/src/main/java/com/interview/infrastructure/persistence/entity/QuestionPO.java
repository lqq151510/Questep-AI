package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class QuestionPO {
    private Long id;
    private Long materialId;
    private Long creatorUserId;
    private String questionType;
    private String stemText;
    private String referenceAnswer;
    private String analysisText;
    private Integer difficulty;
    private String sourceType;
    private String modelName;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getStemText() { return stemText; }
    public void setStemText(String stemText) { this.stemText = stemText; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
    public String getAnalysisText() { return analysisText; }
    public void setAnalysisText(String analysisText) { this.analysisText = analysisText; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
