package com.ecommerce.auth.exception;

public class InvalidAuthenticatedUserException extends RuntimeException {

    public InvalidAuthenticatedUserException(Throwable cause) {
        super("The access token does not identify a valid user", cause);
    }
}
