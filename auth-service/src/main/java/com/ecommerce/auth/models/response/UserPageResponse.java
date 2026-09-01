package com.ecommerce.auth.models.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static UserPageResponse from(Page<UserResponse> users) {
        return new UserPageResponse(
                users.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }
}
