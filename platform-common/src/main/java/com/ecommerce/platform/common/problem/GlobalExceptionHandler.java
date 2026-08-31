package com.ecommerce.platform.common.problem;

import com.ecommerce.platform.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

/**
 * Shared RFC 9457 safety net. Domain-specific advice runs first; this advice
 * standardizes Spring MVC and unexpected failures before Boot's default advice.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailsFactory problemDetailsFactory;

    public GlobalExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<String> violations = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": "
                        + safeValidationMessage(violation.getMessage()))
                .sorted()
                .toList();
        ProblemDetail problem = validationProblem(violations, request);
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindingFailure(
            BindException exception,
            HttpServletRequest request) {
        ProblemDetail problem = validationProblem(fieldViolations(exception), request);
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        logUnexpected(exception, request);
        ProblemDetail problem = problemDetailsFactory.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "Internal server error",
                "An unexpected error occurred",
                request);
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    /**
     * ResponseEntityExceptionHandler owns Spring MVC's framework exception
     * mappings. Customizing this hook prevents Boot's default Problem Details
     * advice from bypassing the platform response contract.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest webRequest) {
        HttpServletRequest request = servletRequest(webRequest);
        if (webRequest instanceof ServletWebRequest servletWebRequest
                && servletWebRequest.getResponse() != null
                && servletWebRequest.getResponse().isCommitted()) {
            return null;
        }

        ProblemDetail problem = frameworkProblem(exception, status, request);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problem, responseHeaders, status);
    }

    private ProblemDetail frameworkProblem(
            Exception exception,
            HttpStatusCode status,
            HttpServletRequest request) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            return validationProblem(fieldViolations(validationException), request);
        }
        if (exception instanceof HandlerMethodValidationException) {
            return problemDetailsFactory.create(
                    status,
                    "validation_failed",
                    "Validation failed",
                    "One or more request values failed validation",
                    request);
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return problemDetailsFactory.create(
                    status,
                    "malformed_json",
                    "Malformed request",
                    "The request body is missing or contains malformed JSON",
                    request);
        }
        if (exception instanceof MissingRequestValueException) {
            return problemDetailsFactory.create(
                    status,
                    "missing_request_value",
                    "Missing request value",
                    "A required request parameter, header, cookie, or path value is missing",
                    request);
        }
        if (exception instanceof NoResourceFoundException) {
            return problemDetailsFactory.create(
                    status,
                    "resource_not_found",
                    "Resource not found",
                    "The requested resource does not exist",
                    request);
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return problemDetailsFactory.create(
                    status,
                    "method_not_allowed",
                    "Method not allowed",
                    "The HTTP method is not supported for this resource",
                    request);
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return problemDetailsFactory.create(
                    status,
                    "unsupported_media_type",
                    "Unsupported media type",
                    "The request content type is not supported",
                    request);
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return problemDetailsFactory.create(
                    status,
                    "not_acceptable",
                    "Not acceptable",
                    "The requested response media type is not supported",
                    request);
        }
        if (exception instanceof TypeMismatchException && status.is4xxClientError()) {
            return problemDetailsFactory.create(
                    status,
                    "type_mismatch",
                    "Invalid parameter type",
                    "A request value has an invalid type",
                    request);
        }
        if (status.is5xxServerError()) {
            logUnexpected(exception, request);
            return problemDetailsFactory.create(
                    status,
                    "internal_error",
                    "Internal server error",
                    "An unexpected error occurred",
                    request);
        }
        return problemDetailsFactory.create(
                status,
                "request_failed",
                "Request failed",
                "The request could not be processed",
                request);
    }

    private ProblemDetail validationProblem(List<String> violations, HttpServletRequest request) {
        ProblemDetail problem = problemDetailsFactory.create(
                HttpStatus.BAD_REQUEST,
                "validation_failed",
                "Validation failed",
                violations.isEmpty() ? "Request validation failed" : String.join("; ", violations),
                request);
        if (!violations.isEmpty()) {
            problem.setProperty("errors", violations);
        }
        return problem;
    }

    private static List<String> fieldViolations(BindException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.getField() + ": "
                        + safeValidationMessage(error.getDefaultMessage()))
                .toList();
    }

    private static HttpServletRequest servletRequest(WebRequest webRequest) {
        return webRequest instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest()
                : null;
    }

    private static ResponseEntity<ProblemDetail> problemResponse(
            HttpStatus status,
            ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String safeValidationMessage(String message) {
        return message == null || message.isBlank() ? "invalid value" : message;
    }

    private static void logUnexpected(Exception exception, HttpServletRequest request) {
        String correlationId = request == null ? null : CorrelationIdFilter.currentCorrelationId(request);
        log.error("Unhandled request failure correlationId={} exceptionType={}",
                correlationId,
                exception.getClass().getName(),
                exception);
    }
}
