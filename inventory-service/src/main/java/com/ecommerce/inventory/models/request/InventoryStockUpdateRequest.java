package com.ecommerce.inventory.models.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryStockUpdateRequest(
        @NotNull(message = "availableQuantity is required")
        @PositiveOrZero(message = "availableQuantity must be zero or greater")
        Long availableQuantity
) {
}
