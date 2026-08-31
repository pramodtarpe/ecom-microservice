package com.ecommerce.auth.service;

import com.ecommerce.auth.api.TokenResponse;
import com.ecommerce.auth.domain.AppUser;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.repository.AppUserRepository;
import com.ecommerce.auth.config.AuthProperties;
import com.ecommerce.platform.common.security.JwtSecurityProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository repository;
    private final JwtEncoder jwtEncoder;
    private final JwtSecurityProperties jwtProperties;
    private final AuthProperties authProperties;
    private final Clock clock;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserRepository repository,
            JwtEncoder jwtEncoder,
            JwtSecurityProperties jwtProperties,
            AuthProperties authProperties
    ) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password)
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException(exception);
        }

        AppUser user = repository.findByEmail(normalizedEmail)
                .filter(AppUser::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(authProperties.jwt().accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .audience(List.of(jwtProperties.getAudience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("userId", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(
                accessToken,
                "Bearer",
                expiresAt,
                authProperties.jwt().accessTokenTtl().toSeconds()
        );
    }
}
