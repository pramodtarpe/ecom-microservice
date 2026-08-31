package com.ecommerce.inventory.exception;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String sku) {
        super("No inventory found for SKU " + sku);
    }
}

