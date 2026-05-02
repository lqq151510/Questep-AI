package com.interview.domain.model;

public record User(Long id, String username, String passwordHash, Integer status) {
}
