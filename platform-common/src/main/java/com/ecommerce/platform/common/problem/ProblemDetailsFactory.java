package com.ecommerce.platform.common.problem;

import com.ecommerce.platform.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Produces a consistent RFC 9457 problem response across services.
 */
public class ProblemDetailsFactory {

    private static final URI ROOT_INSTANCE = URI.create("/");

    private final Clock clock;

    public ProblemDetailsFactory() {
        this(Clock.systemUTC());
    }

    public ProblemDetailsFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ProblemDetail create(
            HttpStatusCode status,
            String code,
            String title,
            String detail,
            HttpServletRequest request) {

        Objects.requireNonNull(status, "status must not be null");
        String normalizedCode = normalizeCode(code);
        String resolvedTitle = title == null || title.isBlank() ? defaultTitle(status) : title;
        String resolvedDetail = detail == null || detail.isBlank() ? resolvedTitle : detail;

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, resolvedDetail);
        problem.setType(URI.create("urn:ecommerce:problem:" + normalizedCode));
        problem.setTitle(resolvedTitle);
        problem.setInstance(resolveInstance(request));
        problem.setProperty("code", normalizedCode);
        problem.setProperty("timestamp", Instant.now(clock));
        problem.setProperty("correlationId", resolveCorrelationId(request));
        return problem;
    }

    public ProblemDetail create(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request) {
        return create(status, code, status.getReasonPhrase(), detail, request);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "unexpected_error";
        }

        String normalized = code.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "unexpected_error" : normalized;
    }

    private static String defaultTitle(HttpStatusCode status) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        return resolvedStatus == null ? "Request failed" : resolvedStatus.getReasonPhrase();
    }

    private static URI resolveInstance(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null || request.getRequestURI().isBlank()) {
            return ROOT_INSTANCE;
        }
        try {
            return URI.create(request.getRequestURI());
        } catch (IllegalArgumentException ignored) {
            return ROOT_INSTANCE;
        }
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        if (request != null) {
            String correlationId = CorrelationIdFilter.currentCorrelationId(request);
            if (correlationId != null) {
                return correlationId;
            }
        }
        return UUID.randomUUID().toString();
    }
}
