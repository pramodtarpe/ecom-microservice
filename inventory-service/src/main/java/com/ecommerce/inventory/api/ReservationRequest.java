package com.ecommerce.inventory.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Long quantity
) {
}

