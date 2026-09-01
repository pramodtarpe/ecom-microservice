package com.ecommerce.productservice.controller;

import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
import com.ecommerce.productservice.exception.DuplicateProductSkuException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private final ProblemDetailsFactory problems;

    public ApiExceptionHandler(ProblemDetailsFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail handleNotFound(ProductNotFoundException exception, HttpServletRequest request) {
        return problems.create(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "Product not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({DuplicateProductSkuException.class, DataIntegrityViolationException.class})
    ProblemDetail handleConflict(Exception exception, HttpServletRequest request) {
        String detail = exception instanceof DuplicateProductSkuException
                ? exception.getMessage()
                : "The product conflicts with an existing record";
        return problems.create(
                HttpStatus.CONFLICT,
                "PRODUCT_CONFLICT",
                "Product conflict",
                detail,
                request
        );
    }
}
