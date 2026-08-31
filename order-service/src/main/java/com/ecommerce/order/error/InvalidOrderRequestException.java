package com.ecommerce.order.error;

public class InvalidOrderRequestException extends RuntimeException {

    public InvalidOrderRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
