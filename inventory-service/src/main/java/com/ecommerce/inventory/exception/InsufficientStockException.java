package com.ecommerce.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    private final long requestedQuantity;
    private final long availableQuantity;

    public InsufficientStockException(String sku, long requestedQuantity, long availableQuantity) {
        super("Cannot reserve " + requestedQuantity + " units of " + sku
                + "; only " + availableQuantity + " units are available");
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public long getRequestedQuantity() {
        return requestedQuantity;
    }

    public long getAvailableQuantity() {
        return availableQuantity;
    }
}

