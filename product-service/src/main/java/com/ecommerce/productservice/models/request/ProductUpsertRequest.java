package com.ecommerce.productservice.models.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpsertRequest(
        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must be at most 64 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
                message = "sku must start with a letter or number and contain only letters, numbers, '.', '_' or '-'"
        )
        String sku,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Size(max = 2_000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "price must have at most 17 integer digits and 2 decimal places")
        BigDecimal price,

        boolean active
) {
}
