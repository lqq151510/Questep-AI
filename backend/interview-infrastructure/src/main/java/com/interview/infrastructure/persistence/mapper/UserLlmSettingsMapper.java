package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.UserLlmSettings;
import com.interview.infrastructure.persistence.entity.UserLlmSettingsPO;
import org.apache.ibatis.annotations.Param;

public interface UserLlmSettingsMapper {

    UserLlmSettings selectByUserId(@Param("userId") Long userId);

    int upsert(UserLlmSettingsPO po);
}
