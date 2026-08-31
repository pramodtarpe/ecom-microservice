package com.ecommerce.auth.api;

import com.ecommerce.auth.service.AuthenticatedUser;
import com.ecommerce.auth.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.listUsers(page, size, AuthenticatedUser.id(jwt));
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.changeRole(id, request.role(), AuthenticatedUser.id(jwt));
    }

    @PatchMapping("/{id}/enabled")
    public UserResponse changeEnabled(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody UpdateEnabledRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.changeEnabled(id, request.enabled(), AuthenticatedUser.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) long id, @AuthenticationPrincipal Jwt jwt) {
        userService.delete(id, AuthenticatedUser.id(jwt));
    }
}
