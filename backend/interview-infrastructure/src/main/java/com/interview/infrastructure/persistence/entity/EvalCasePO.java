package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class EvalCasePO {

    private Long id;
    private String caseKey;
    private String category;
    private String description;
    private String input;
    private String expectedKeywords;
    private String expectedStructure;
    private Double minScore;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseKey() { return caseKey; }
    public void setCaseKey(String caseKey) { this.caseKey = caseKey; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedKeywords() { return expectedKeywords; }
    public void setExpectedKeywords(String expectedKeywords) { this.expectedKeywords = expectedKeywords; }
    public String getExpectedStructure() { return expectedStructure; }
    public void setExpectedStructure(String expectedStructure) { this.expectedStructure = expectedStructure; }
    public Double getMinScore() { return minScore; }
    public void setMinScore(Double minScore) { this.minScore = minScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
