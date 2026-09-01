package com.ecommerce.auth.models.response;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        long expiresInSeconds
) {
}
