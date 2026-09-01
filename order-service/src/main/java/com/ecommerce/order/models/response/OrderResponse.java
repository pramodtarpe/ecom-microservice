package com.ecommerce.order.models.response;

import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.PurchaseOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerEmail,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        List<Item> items) {

    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(Item::from).toList());
    }

    public record Item(
            Long id,
            String sku,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {

        static Item from(OrderItem item) {
            return new Item(
                    item.getId(),
                    item.getSku(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal());
        }
    }
}
