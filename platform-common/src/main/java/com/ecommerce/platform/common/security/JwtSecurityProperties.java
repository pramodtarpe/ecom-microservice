package com.ecommerce.platform.common.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;

/**
 * Shared symmetric JWT settings used by token issuers and resource servers.
 */
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtSecurityProperties {

    public static final String DEFAULT_ISSUER = "ecommerce-auth";
    public static final String DEFAULT_AUDIENCE = "ecommerce-api";
    public static final int MINIMUM_SECRET_BYTES = 32;

    @NotBlank(message = "security.jwt.secret must be configured")
    private String secret;

    @NotBlank
    private String issuer = DEFAULT_ISSUER;

    @NotBlank
    private String audience = DEFAULT_AUDIENCE;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    @AssertTrue(message = "security.jwt.secret must contain at least 32 UTF-8 bytes")
    public boolean isSecretAtLeast32Bytes() {
        return secret != null
                && secret.getBytes(StandardCharsets.UTF_8).length >= MINIMUM_SECRET_BYTES;
    }
}
