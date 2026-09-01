package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.exception.DuplicateSkuException;
import com.ecommerce.inventory.exception.InsufficientReservedQuantityException;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.InvalidInventoryRequestException;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
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
public class InventoryExceptionHandler {

    private final ProblemDetailsFactory problems;

    public InventoryExceptionHandler(ProblemDetailsFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    ProblemDetail handleNotFound(InventoryNotFoundException exception, HttpServletRequest request) {
        return problems.create(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateSkuException.class)
    ProblemDetail handleDuplicate(DuplicateSkuException exception, HttpServletRequest request) {
        return problems.create(HttpStatus.CONFLICT, "DUPLICATE_SKU", exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException exception, HttpServletRequest request) {
        ProblemDetail problem = problems.create(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_STOCK",
                exception.getMessage(),
                request
        );
        problem.setProperty("requestedQuantity", exception.getRequestedQuantity());
        problem.setProperty("availableQuantity", exception.getAvailableQuantity());
        return problem;
    }

    @ExceptionHandler(InsufficientReservedQuantityException.class)
    ProblemDetail handleInsufficientReservedQuantity(
            InsufficientReservedQuantityException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problems.create(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_RESERVED_QUANTITY",
                exception.getMessage(),
                request
        );
        problem.setProperty("requestedQuantity", exception.getRequestedQuantity());
        problem.setProperty("reservedQuantity", exception.getReservedQuantity());
        return problem;
    }

    @ExceptionHandler(InvalidInventoryRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidInventoryRequestException exception, HttpServletRequest request) {
        return problems.create(HttpStatus.BAD_REQUEST, "INVALID_INVENTORY_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return problems.create(
                HttpStatus.CONFLICT,
                "INVENTORY_DATA_CONFLICT",
                "The request conflicts with existing inventory data",
                request
        );
    }
}
