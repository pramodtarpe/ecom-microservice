package com.ecommerce.auth.exception;

public class AdminAccessRevokedException extends RuntimeException {

    public AdminAccessRevokedException() {
        super("The authenticated account is no longer an enabled administrator");
    }
}
