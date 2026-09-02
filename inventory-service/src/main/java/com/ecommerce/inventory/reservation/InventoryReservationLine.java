package com.ecommerce.inventory.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "inventory_reservation_lines")
public class InventoryReservationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private InventoryReservation reservation;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false)
    private long quantity;

    protected InventoryReservationLine() {
    }

    InventoryReservationLine(InventoryReservation reservation, String sku, long quantity) {
        this.reservation = Objects.requireNonNull(reservation, "reservation");
        this.sku = Objects.requireNonNull(sku, "sku");
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public long getQuantity() {
        return quantity;
    }
}
