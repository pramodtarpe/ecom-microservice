package com.ecommerce.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    private static final String MESSAGE = "The email or password is incorrect";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }

    public InvalidCredentialsException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
