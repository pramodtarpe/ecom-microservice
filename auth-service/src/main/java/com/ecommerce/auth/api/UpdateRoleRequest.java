package com.ecommerce.auth.api;

import com.ecommerce.auth.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {
}
