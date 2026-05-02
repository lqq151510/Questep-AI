package com.interview.domain.repository;

import com.interview.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    User save(String username, String email, String passwordHash);
}
