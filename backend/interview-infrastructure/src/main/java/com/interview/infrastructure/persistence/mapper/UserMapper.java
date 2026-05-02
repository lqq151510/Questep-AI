package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.User;
import com.interview.infrastructure.persistence.entity.UserPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    int insert(UserPO userPO);

    int updateById(UserPO userPO);

    User selectByUsername(String username);

    User selectByEmail(String email);

    User selectById(Long id);

    List<User> selectByCondition(UserPO condition);

    List<User> selectByIds(@Param("ids") List<Long> ids);

    int deleteById(Long id);
}
