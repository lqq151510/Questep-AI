package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.UserLlmSettings;
import com.interview.domain.repository.UserLlmSettingsRepository;
import com.interview.infrastructure.persistence.entity.UserLlmSettingsPO;
import com.interview.infrastructure.persistence.mapper.UserLlmSettingsMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserLlmSettingsRepositoryImpl implements UserLlmSettingsRepository {

    private final UserLlmSettingsMapper mapper;

    public UserLlmSettingsRepositoryImpl(UserLlmSettingsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserLlmSettings> findByUserId(Long userId) {
        return Optional.ofNullable(mapper.selectByUserId(userId));
    }

    @Override
    public UserLlmSettings saveOrUpdate(
            Long userId,
            String providerName,
            String modelName,
            String baseUrl,
            String apiKey,
            Integer enabled
    ) {
        UserLlmSettingsPO po = new UserLlmSettingsPO();
        po.setUserId(userId);
        po.setProviderName(providerName);
        po.setModelName(modelName);
        po.setBaseUrl(baseUrl);
        po.setApiKey(apiKey);
        po.setEnabled(enabled == null ? 1 : enabled);
        mapper.upsert(po);
        return mapper.selectByUserId(userId);
    }
}
