package com.ecommerce.order.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clients")
public record DownstreamClientsProperties(
        @Valid @NotNull Service catalog) {

    public record Service(
            @NotNull URI baseUrl,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout) {

        public Service {
            requirePositive(connectTimeout, "connectTimeout");
            requirePositive(readTimeout, "readTimeout");
        }

        private static void requirePositive(Duration duration, String propertyName) {
            if (duration != null && (duration.isZero() || duration.isNegative())) {
                throw new IllegalArgumentException(propertyName + " must be greater than zero");
            }
        }
    }
}
