package com.ecommerce.productservice.exception;

public class DuplicateProductSkuException extends RuntimeException {

    public DuplicateProductSkuException(String sku) {
        super("A product with SKU '" + sku + "' already exists");
    }
}
