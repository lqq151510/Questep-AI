package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class EvalRunPO {

    private Long id;
    private String runKey;
    private Long promptVersionId;
    private Integer totalCases;
    private Integer passedCases;
    private Double avgScore;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunKey() { return runKey; }
    public void setRunKey(String runKey) { this.runKey = runKey; }
    public Long getPromptVersionId() { return promptVersionId; }
    public void setPromptVersionId(Long promptVersionId) { this.promptVersionId = promptVersionId; }
    public Integer getTotalCases() { return totalCases; }
    public void setTotalCases(Integer totalCases) { this.totalCases = totalCases; }
    public Integer getPassedCases() { return passedCases; }
    public void setPassedCases(Integer passedCases) { this.passedCases = passedCases; }
    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
