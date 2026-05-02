package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.User;
import com.interview.infrastructure.persistence.entity.UserPO;

public interface UserMapper {
    int insert(UserPO userPO);
    User selectByUsername(String username);
    User selectByEmail(String email);
    User selectById(Long id);
}
