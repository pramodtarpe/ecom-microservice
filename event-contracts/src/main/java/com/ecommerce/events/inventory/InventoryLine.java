package com.ecommerce.events.inventory;

import java.util.Objects;
import java.util.regex.Pattern;

/** A canonical SKU and quantity carried by an inventory event. */
public record InventoryLine(String sku, long quantity) {

    private static final int MAX_SKU_LENGTH = 64;
    private static final Pattern SKU_PATTERN =
            Pattern.compile("[A-Z0-9][A-Z0-9._-]*");

    public InventoryLine {
        Objects.requireNonNull(sku, "sku must not be null");
        if (sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (!sku.equals(sku.strip())) {
            throw new IllegalArgumentException("sku must not contain surrounding whitespace");
        }
        if (sku.length() > MAX_SKU_LENGTH || !SKU_PATTERN.matcher(sku).matches()) {
            throw new IllegalArgumentException("sku must be a canonical uppercase SKU");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }
}
