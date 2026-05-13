package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class EvalResultPO {

    private Long id;
    private Long runId;
    private Long caseId;
    private String actualOutput;
    private Double score;
    private String keywordHits;
    private Boolean structureValid;
    private Long durationMs;
    private String errorMsg;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getKeywordHits() { return keywordHits; }
    public void setKeywordHits(String keywordHits) { this.keywordHits = keywordHits; }
    public Boolean getStructureValid() { return structureValid; }
    public void setStructureValid(Boolean structureValid) { this.structureValid = structureValid; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
