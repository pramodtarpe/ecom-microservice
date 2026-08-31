package com.ecommerce.inventory.service;

import com.ecommerce.inventory.exception.InvalidInventoryRequestException;

import java.util.Locale;
import java.util.regex.Pattern;

final class SkuNormalizer {

    private static final int MAX_LENGTH = 64;
    private static final Pattern VALID_SKU = Pattern.compile("[A-Z0-9][A-Z0-9._-]*");

    private SkuNormalizer() {
    }

    static String normalize(String rawSku) {
        if (rawSku == null || rawSku.isBlank()) {
            throw new InvalidInventoryRequestException("sku is required");
        }

        String normalized = rawSku.strip().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH || !VALID_SKU.matcher(normalized).matches()) {
            throw new InvalidInventoryRequestException(
                    "sku must be at most 64 characters and contain only letters, numbers, '.', '_' or '-'"
            );
        }
        return normalized;
    }
}

