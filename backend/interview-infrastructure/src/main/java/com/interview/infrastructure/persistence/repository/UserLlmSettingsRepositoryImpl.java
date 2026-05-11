package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.UserLlmSettings;
import com.interview.domain.repository.UserLlmSettingsRepository;
import com.interview.infrastructure.persistence.entity.UserLlmSettingsPO;
import com.interview.infrastructure.persistence.mapper.UserLlmSettingsMapper;
import com.interview.infrastructure.security.ApiKeyCryptoService;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserLlmSettingsRepositoryImpl implements UserLlmSettingsRepository {

    private final UserLlmSettingsMapper mapper;
    private final ApiKeyCryptoService apiKeyCryptoService;

    public UserLlmSettingsRepositoryImpl(UserLlmSettingsMapper mapper, ApiKeyCryptoService apiKeyCryptoService) {
        this.mapper = mapper;
        this.apiKeyCryptoService = apiKeyCryptoService;
    }

    @Override
    public Optional<UserLlmSettings> findByUserId(Long userId) {
        return Optional.ofNullable(mapper.selectByUserId(userId))
                .map(this::toDomain);
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
        po.setApiKey(apiKeyCryptoService.encryptForStorage(apiKey));
        po.setEnabled(enabled == null ? 1 : enabled);
        mapper.upsert(po);
        return Optional.ofNullable(mapper.selectByUserId(userId))
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("Failed to load user llm settings after save, userId=" + userId));
    }

    private UserLlmSettings toDomain(UserLlmSettingsPO po) {
        return new UserLlmSettings(
                po.getId(),
                po.getUserId(),
                po.getProviderName(),
                po.getModelName(),
                po.getBaseUrl(),
                apiKeyCryptoService.decryptFromStorage(po.getApiKey()),
                po.getEnabled(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
