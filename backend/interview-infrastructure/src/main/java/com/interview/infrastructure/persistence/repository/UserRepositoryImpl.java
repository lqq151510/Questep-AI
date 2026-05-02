package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import com.interview.infrastructure.persistence.entity.UserPO;
import com.interview.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    public UserRepositoryImpl(UserMapper userMapper) { this.userMapper = userMapper; }
    public Optional<User> findByUsername(String username) { return Optional.ofNullable(userMapper.selectByUsername(username)); }
    public Optional<User> findByEmail(String email) { return Optional.ofNullable(userMapper.selectByEmail(email)); }
    public Optional<User> findById(Long id) { return Optional.ofNullable(userMapper.selectById(id)); }

    public User save(String username, String email, String passwordHash) {
        UserPO po = new UserPO();
        po.setUsername(username);
        po.setEmail(email);
        po.setPasswordHash(passwordHash);
        po.setDisplayName(username);
        po.setStatus(1);
        userMapper.insert(po);
        return userMapper.selectById(po.getId());
    }
}
