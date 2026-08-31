package com.ecommerce.platform.common.security;

import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Writes authentication failures using the platform problem-detail contract. */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;

    public ProblemDetailAuthenticationEntryPoint(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper) {
        this.problemDetailsFactory = Objects.requireNonNull(problemDetailsFactory);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problem = problemDetailsFactory.create(
                HttpStatus.UNAUTHORIZED,
                "authentication_required",
                "Authentication required",
                "A valid bearer token is required to access this resource",
                request);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
