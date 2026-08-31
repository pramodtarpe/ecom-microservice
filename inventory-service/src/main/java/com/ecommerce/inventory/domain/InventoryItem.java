package com.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_item_sku", columnNames = "sku")
)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, updatable = false)
    private String sku;

    @Column(nullable = false)
    private long availableQuantity;

    @Column(nullable = false)
    private long reservedQuantity;

    @Version
    private long version;

    protected InventoryItem() {
    }

    private InventoryItem(String sku, long availableQuantity) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }

    public static InventoryItem create(String sku, long availableQuantity) {
        return new InventoryItem(sku, availableQuantity);
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public long getAvailableQuantity() {
        return availableQuantity;
    }

    public long getReservedQuantity() {
        return reservedQuantity;
    }

    public long getVersion() {
        return version;
    }

    public void replaceAvailableQuantity(long availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public void reserve(long quantity) {
        availableQuantity = Math.subtractExact(availableQuantity, quantity);
        reservedQuantity = Math.addExact(reservedQuantity, quantity);
    }

    public void release(long quantity) {
        reservedQuantity = Math.subtractExact(reservedQuantity, quantity);
        availableQuantity = Math.addExact(availableQuantity, quantity);
    }
}

