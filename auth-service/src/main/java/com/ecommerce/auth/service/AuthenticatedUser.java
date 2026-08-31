package com.ecommerce.auth.service;

import com.ecommerce.auth.exception.InvalidAuthenticatedUserException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static long id(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException exception) {
            throw new InvalidAuthenticatedUserException(exception);
        }
    }
}
