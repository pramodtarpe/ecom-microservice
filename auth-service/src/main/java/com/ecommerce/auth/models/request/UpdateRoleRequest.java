package com.ecommerce.auth.models.request;

import com.ecommerce.auth.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {
}
