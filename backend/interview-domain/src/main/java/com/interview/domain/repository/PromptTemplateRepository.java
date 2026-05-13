package com.interview.domain.repository;

import com.interview.domain.model.PromptTemplate;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository {

    Optional<PromptTemplate> findActiveByKey(String templateKey);

    List<PromptTemplate> findByKey(String templateKey);

    Optional<PromptTemplate> findById(Long id);

    PromptTemplate save(PromptTemplate template);

    void updateStatusByKeyAndVersion(String templateKey, int version, String status);
}
