package com.ecommerce.order.api;

import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
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
            JwtAuthenticationToken authentication) {
        var response = orderService.create(request, customerIdentity(authentication));
        return ResponseEntity.created(URI.create("/api/orders/" + response.id())).body(response);
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
    public OrderResponse cancel(@PathVariable long id, JwtAuthenticationToken authentication) {
        return orderService.cancel(id, customerIdentity(authentication), isAdmin(authentication));
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
