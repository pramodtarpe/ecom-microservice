package com.ecommerce.auth.api;

import com.ecommerce.auth.exception.AdminAccountProtectionException;
import com.ecommerce.auth.exception.AdminAccessRevokedException;
import com.ecommerce.auth.exception.DuplicateEmailException;
import com.ecommerce.auth.exception.InvalidAuthenticatedUserException;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.exception.LastEnabledAdminException;
import com.ecommerce.auth.exception.UserNotFoundException;
import com.ecommerce.platform.common.problem.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    private final ProblemDetailsFactory problemDetailsFactory;

    public AuthExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
        this.problemDetailsFactory = problemDetailsFactory;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail handleDuplicateEmail(DuplicateEmailException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "email-already-registered",
                "Email address is already registered",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.NOT_FOUND,
                "user-not-found",
                "User not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.UNAUTHORIZED,
                "invalid-credentials",
                "Authentication failed",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidAuthenticatedUserException.class)
    ProblemDetail handleInvalidPrincipal(InvalidAuthenticatedUserException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.UNAUTHORIZED,
                "invalid-authenticated-user",
                "Invalid authenticated user",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({AdminAccountProtectionException.class, LastEnabledAdminException.class})
    ProblemDetail handleAdminProtection(RuntimeException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.CONFLICT,
                "administrator-account-protected",
                "Administrator account is protected",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AdminAccessRevokedException.class)
    ProblemDetail handleAdminAccessRevoked(AdminAccessRevokedException exception, HttpServletRequest request) {
        return problemDetailsFactory.create(
                HttpStatus.FORBIDDEN,
                "administrator-access-revoked",
                "Administrator access revoked",
                exception.getMessage(),
                request
        );
    }
}
