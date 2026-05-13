package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public class PromptTemplatePO {

    private Long id;
    private String templateKey;
    private Integer version;
    private String name;
    private String systemPrompt;
    private String userTemplate;
    private String variables;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserTemplate() { return userTemplate; }
    public void setUserTemplate(String userTemplate) { this.userTemplate = userTemplate; }
    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
