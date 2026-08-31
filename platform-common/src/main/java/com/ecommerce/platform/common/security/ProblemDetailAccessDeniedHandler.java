package com.ecommerce.platform.common.security;

import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Writes authorization failures using the platform problem-detail contract. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;

    public ProblemDetailAccessDeniedHandler(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper) {
        this.problemDetailsFactory = Objects.requireNonNull(problemDetailsFactory);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problem = problemDetailsFactory.create(
                HttpStatus.FORBIDDEN,
                "access_denied",
                "Access denied",
                "You do not have permission to access this resource",
                request);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
