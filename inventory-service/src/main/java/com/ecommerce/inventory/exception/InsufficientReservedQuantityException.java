package com.ecommerce.inventory.exception;

public class InsufficientReservedQuantityException extends RuntimeException {

    private final long requestedQuantity;
    private final long reservedQuantity;

    public InsufficientReservedQuantityException(String sku, long requestedQuantity, long reservedQuantity) {
        super("Cannot release " + requestedQuantity + " units of " + sku
                + "; only " + reservedQuantity + " units are reserved");
        this.requestedQuantity = requestedQuantity;
        this.reservedQuantity = reservedQuantity;
    }

    public long getRequestedQuantity() {
        return requestedQuantity;
    }

    public long getReservedQuantity() {
        return reservedQuantity;
    }
}

