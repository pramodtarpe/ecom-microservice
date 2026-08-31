package com.ecommerce.inventory.exception;

public class InvalidInventoryRequestException extends RuntimeException {

    public InvalidInventoryRequestException(String message) {
        super(message);
    }
}

