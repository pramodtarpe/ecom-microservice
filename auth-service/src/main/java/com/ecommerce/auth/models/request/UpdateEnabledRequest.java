package com.ecommerce.auth.models.request;

import jakarta.validation.constraints.NotNull;

public record UpdateEnabledRequest(@NotNull Boolean enabled) {
}
