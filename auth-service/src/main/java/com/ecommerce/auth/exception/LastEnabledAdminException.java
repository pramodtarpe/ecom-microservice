package com.ecommerce.auth.exception;

public class LastEnabledAdminException extends RuntimeException {

    public LastEnabledAdminException() {
        super("At least one enabled administrator account must remain");
    }
}
