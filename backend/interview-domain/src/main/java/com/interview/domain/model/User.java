package com.interview.domain.model;

import java.time.LocalDateTime;

public record User(
        Long id,
        String username,
        String email,
        String passwordHash,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_INACTIVE = 0;
}
