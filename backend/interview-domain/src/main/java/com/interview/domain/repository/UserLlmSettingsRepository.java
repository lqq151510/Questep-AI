package com.interview.domain.repository;

import com.interview.domain.model.UserLlmSettings;

import java.util.Optional;

public interface UserLlmSettingsRepository {

    Optional<UserLlmSettings> findByUserId(Long userId);

    UserLlmSettings saveOrUpdate(
            Long userId,
            String providerName,
            String modelName,
            String baseUrl,
            String apiKey,
            Integer enabled
    );
}
