package com.interview.infrastructure.persistence.mapper;

import com.interview.infrastructure.persistence.entity.UserLlmSettingsPO;
import org.apache.ibatis.annotations.Param;

public interface UserLlmSettingsMapper {

    UserLlmSettingsPO selectByUserId(@Param("userId") Long userId);

    int upsert(UserLlmSettingsPO po);
}
