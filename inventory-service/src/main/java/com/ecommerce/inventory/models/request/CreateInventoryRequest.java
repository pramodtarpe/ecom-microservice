package com.ecommerce.inventory.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateInventoryRequest(
        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must contain at most 64 characters")
        String sku,

        @NotNull(message = "availableQuantity is required")
        @PositiveOrZero(message = "availableQuantity must be zero or greater")
        Long availableQuantity
) {
}
