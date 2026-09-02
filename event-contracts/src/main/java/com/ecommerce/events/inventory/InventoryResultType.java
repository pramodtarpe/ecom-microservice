package com.ecommerce.events.inventory;

/** Outcomes emitted by the inventory reservation workflow. */
public enum InventoryResultType {
    RESERVED,
    REJECTED,
    RELEASED
}
