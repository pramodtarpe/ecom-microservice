package com.ecommerce.productservice.exception;

public class ProductNotFoundException extends RuntimeException {

    private ProductNotFoundException(String message) {
        super(message);
    }

    public static ProductNotFoundException forId(Long id) {
        return new ProductNotFoundException("Product with id " + id + " was not found");
    }

    public static ProductNotFoundException forSku(String sku) {
        return new ProductNotFoundException("Product with SKU '" + sku + "' was not found");
    }
}
