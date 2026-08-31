package com.ecommerce.order.error;

public class InventoryReservationException extends RuntimeException {

    public InventoryReservationException(String sku, Throwable cause) {
        super("Inventory could not reserve the requested quantity for SKU " + sku, cause);
    }
}
