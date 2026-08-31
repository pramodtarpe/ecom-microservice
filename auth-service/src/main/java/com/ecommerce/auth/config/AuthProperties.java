package com.ecommerce.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        @Min(10) @Max(16) int bcryptStrength,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Seed seed
) {

    public AuthProperties {
        if (jwt != null && jwt.accessTokenTtl() != null
                && (jwt.accessTokenTtl().isNegative() || jwt.accessTokenTtl().isZero())) {
            throw new IllegalArgumentException("auth.jwt.access-token-ttl must be positive");
        }
    }

    public record Jwt(@NotNull Duration accessTokenTtl) {
    }

    public record Seed(
            boolean enabled,
            @Valid @NotNull SeedAccount admin,
            @Valid @NotNull SeedAccount user
    ) {
    }

    public record SeedAccount(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 100) String displayName,
            @NotBlank @Size(min = 12, max = 72) String password
    ) {
    }
}
