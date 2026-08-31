package com.ecommerce.order.error;

public class ProductUnavailableException extends RuntimeException {

    public ProductUnavailableException(String sku) {
        super("No orderable catalog product exists for SKU " + sku);
    }
}
