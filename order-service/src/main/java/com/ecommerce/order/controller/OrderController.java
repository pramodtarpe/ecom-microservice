package com.ecommerce.order.controller;

import com.ecommerce.order.models.request.CreateOrderRequest;
import com.ecommerce.order.models.response.OrderResponse;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.platform.common.web.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            JwtAuthenticationToken authentication,
            HttpServletRequest httpRequest) {
        var response = orderService.create(
                request,
                customerIdentity(authentication),
                CorrelationIdFilter.currentCorrelationId(httpRequest));
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<OrderResponse> findAll(JwtAuthenticationToken authentication) {
        return orderService.findAll(customerIdentity(authentication), isAdmin(authentication));
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable long id, JwtAuthenticationToken authentication) {
        return orderService.findById(id, customerIdentity(authentication), isAdmin(authentication));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(
            @PathVariable long id,
            JwtAuthenticationToken authentication,
            HttpServletRequest httpRequest) {
        var response = orderService.cancel(
                id,
                customerIdentity(authentication),
                isAdmin(authentication),
                CorrelationIdFilter.currentCorrelationId(httpRequest));
        return response.status() == OrderStatus.CANCELLED
                ? ResponseEntity.ok(response)
                : ResponseEntity.accepted().body(response);
    }

    private String customerIdentity(JwtAuthenticationToken authentication) {
        var email = authentication.getToken().getClaimAsString("email");
        if (StringUtils.hasText(email)) {
            return email.trim().toLowerCase(Locale.ROOT);
        }
        var subject = authentication.getToken().getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new IllegalStateException("Authenticated JWT has no subject or email claim");
        }
        return subject.trim();
    }

    private boolean isAdmin(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
