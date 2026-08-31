package com.ecommerce.platform.common.config;

import com.ecommerce.platform.common.logging.LoggingAspect;
import com.ecommerce.platform.common.problem.GlobalExceptionHandler;
import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
import com.ecommerce.platform.common.security.JwtSecurityProperties;
import com.ecommerce.platform.common.security.ProblemDetailAccessDeniedHandler;
import com.ecommerce.platform.common.security.ProblemDetailAuthenticationEntryPoint;
import com.ecommerce.platform.common.web.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, JwtDecoder.class, ObjectMapper.class})
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class PlatformCommonAutoConfiguration {

    @Bean(name = "jwtSecretKey")
    @ConditionalOnMissingBean(name = "jwtSecretKey")
    public SecretKey jwtSecretKey(JwtSecurityProperties properties) {
        return new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(
            @Qualifier("jwtSecretKey") SecretKey jwtSecretKey,
            JwtSecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.getIssuer());
        OAuth2TokenValidator<Jwt> audienceValidator = jwt ->
                jwt.getAudience().contains(properties.getAudience())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                                "invalid_token",
                                "The required audience is missing",
                                null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesClaimAuthorities());
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailsFactory problemDetailsFactory() {
        return new ProblemDetailsFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper) {
        return new ProblemDetailAuthenticationEntryPoint(problemDetailsFactory, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper) {
        return new ProblemDetailAccessDeniedHandler(problemDetailsFactory, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
        return new GlobalExceptionHandler(problemDetailsFactory);
    }

    private static Converter<Jwt, Collection<GrantedAuthority>> rolesClaimAuthorities() {
        return jwt -> {
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim == null) {
                return List.of();
            }

            Collection<?> roles = rolesClaim instanceof Collection<?> collection
                    ? collection
                    : List.of(rolesClaim.toString().split("[ ,]+"));
            return roles.stream()
                    .map(Object::toString)
                    .map(String::strip)
                    .filter(role -> !role.isBlank())
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .distinct()
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        };
    }
}
