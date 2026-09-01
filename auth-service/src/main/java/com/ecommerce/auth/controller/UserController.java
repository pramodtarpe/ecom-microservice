package com.ecommerce.auth.controller;

import com.ecommerce.auth.models.response.UserResponse;
import com.ecommerce.auth.service.AuthenticatedUser;
import com.ecommerce.auth.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getUser(AuthenticatedUser.id(jwt));
    }
}
