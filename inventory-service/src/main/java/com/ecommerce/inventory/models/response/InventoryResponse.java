package com.ecommerce.inventory.models.response;

import com.ecommerce.inventory.domain.InventoryItem;

public record InventoryResponse(
        Long id,
        String sku,
        long availableQuantity,
        long reservedQuantity,
        long version
) {
    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(
                item.getId(),
                item.getSku(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getVersion()
        );
    }
}
