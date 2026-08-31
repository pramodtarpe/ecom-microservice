package com.ecommerce.order.config;

import com.ecommerce.platform.common.security.JwtSecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/** Issues narrowly scoped credentials for synchronous order-service dependencies. */
@Component
public class ServiceTokenProvider {

    private static final String SERVICE_SUBJECT = "order-service";
    private static final Duration TOKEN_TTL = Duration.ofSeconds(60);

    private final JwtEncoder jwtEncoder;
    private final JwtSecurityProperties jwtProperties;
    private final Clock clock;

    public ServiceTokenProvider(JwtEncoder jwtEncoder, JwtSecurityProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.clock = Clock.systemUTC();
    }

    public String createToken(String actorSubject) {
        Assert.hasText(actorSubject, "actorSubject must not be blank");

        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .audience(List.of(jwtProperties.getAudience()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(TOKEN_TTL))
                .subject(SERVICE_SUBJECT)
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("SERVICE"))
                .claim("actorSub", actorSubject)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
