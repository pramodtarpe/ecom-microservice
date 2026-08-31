package com.ecommerce.auth.api;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        long expiresInSeconds
) {
}
