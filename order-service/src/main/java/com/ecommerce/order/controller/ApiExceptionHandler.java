package com.ecommerce.order.controller;

import com.ecommerce.order.error.DownstreamServiceException;
import com.ecommerce.order.error.InvalidOrderRequestException;
import com.ecommerce.order.error.OrderNotFoundException;
import com.ecommerce.order.error.ProductUnavailableException;
import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private final ProblemDetailsFactory problemDetailsFactory;

    public ApiExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            OrderNotFoundException exception, HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "order_not_found",
                "Order not found",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(ProductUnavailableException.class)
    ResponseEntity<ProblemDetail> handleProductUnavailable(
            ProductUnavailableException exception, HttpServletRequest request) {
        return response(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "product_unavailable",
                "Product unavailable",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<ProblemDetail> handleDownstreamFailure(
            DownstreamServiceException exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_GATEWAY,
                "downstream_service_failure",
                "Downstream service failure",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(InvalidOrderRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidOrder(
            InvalidOrderRequestException exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "invalid_order",
                "Invalid order",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler({CallNotPermittedException.class, BulkheadFullException.class})
    ResponseEntity<ProblemDetail> handleCapacityFailure(
            RuntimeException exception, HttpServletRequest request) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "downstream_capacity_exhausted",
                "Downstream service temporarily unavailable",
                "A required downstream service cannot accept this request right now",
                request);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemDetailsFactory.create(status, code, title, detail, request));
    }
}
