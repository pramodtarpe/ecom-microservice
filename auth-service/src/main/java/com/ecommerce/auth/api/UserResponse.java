package com.ecommerce.auth.api;

import com.ecommerce.auth.domain.AppUser;
import com.ecommerce.auth.domain.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
