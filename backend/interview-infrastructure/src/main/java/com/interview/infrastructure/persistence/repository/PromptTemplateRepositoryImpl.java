package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.PromptTemplate;
import com.interview.domain.repository.PromptTemplateRepository;
import com.interview.infrastructure.persistence.mapper.PromptTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PromptTemplateRepositoryImpl implements PromptTemplateRepository {

    private final PromptTemplateMapper promptTemplateMapper;

    public PromptTemplateRepositoryImpl(PromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    public Optional<PromptTemplate> findActiveByKey(String templateKey) {
        return Optional.ofNullable(promptTemplateMapper.selectActiveByKey(templateKey));
    }

    @Override
    public List<PromptTemplate> findByKey(String templateKey) {
        return promptTemplateMapper.selectByKey(templateKey);
    }

    @Override
    public Optional<PromptTemplate> findById(Long id) {
        return Optional.ofNullable(promptTemplateMapper.selectById(id));
    }

    @Override
    public PromptTemplate save(PromptTemplate template) {
        promptTemplateMapper.insert(template);
        return promptTemplateMapper.selectById(template.id());
    }

    @Override
    public void updateStatusByKeyAndVersion(String templateKey, int version, String status) {
        promptTemplateMapper.updateStatusByKeyAndVersion(templateKey, version, status);
    }
}
