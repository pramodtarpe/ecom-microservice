package com.ecommerce.inventory.reservation;

import com.ecommerce.events.inventory.InventoryLine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryReservationStatus status;

    @Column(name = "last_sequence", nullable = false)
    private long lastSequence;

    @Column(name = "last_command_id", nullable = false, length = 36)
    private String lastCommandId;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryReservationLine> items = new ArrayList<>();

    protected InventoryReservation() {
    }

    private InventoryReservation(
            long orderId,
            long sequence,
            UUID commandId,
            InventoryReservationStatus status,
            List<InventoryLine> items,
            String failureCode,
            String failureReason) {
        this.orderId = orderId;
        this.lastSequence = sequence;
        this.lastCommandId = Objects.requireNonNull(commandId, "commandId").toString();
        this.status = Objects.requireNonNull(status, "status");
        this.failureCode = failureCode;
        this.failureReason = normalizeReason(failureReason);
        items.forEach(item -> this.items.add(new InventoryReservationLine(
                this, item.sku(), item.quantity())));
    }

    public static InventoryReservation reserved(
            long orderId, long sequence, UUID commandId, List<InventoryLine> items) {
        return new InventoryReservation(
                orderId,
                sequence,
                commandId,
                InventoryReservationStatus.RESERVED,
                items,
                null,
                null);
    }

    public static InventoryReservation rejected(
            long orderId,
            long sequence,
            UUID commandId,
            List<InventoryLine> items,
            String failureCode,
            String failureReason) {
        return new InventoryReservation(
                orderId,
                sequence,
                commandId,
                InventoryReservationStatus.REJECTED,
                items,
                Objects.requireNonNull(failureCode, "failureCode"),
                Objects.requireNonNull(failureReason, "failureReason"));
    }

    public static InventoryReservation releasedTombstone(
            long orderId, long sequence, UUID commandId, List<InventoryLine> items) {
        return new InventoryReservation(
                orderId,
                sequence,
                commandId,
                InventoryReservationStatus.RELEASED,
                items,
                null,
                null);
    }

    public void markReleased(long sequence, UUID commandId) {
        status = InventoryReservationStatus.RELEASED;
        lastSequence = sequence;
        lastCommandId = Objects.requireNonNull(commandId, "commandId").toString();
        failureCode = null;
        failureReason = null;
    }

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public InventoryReservationStatus getStatus() {
        return status;
    }

    public long getLastSequence() {
        return lastSequence;
    }

    public List<InventoryReservationLine> getItems() {
        return Collections.unmodifiableList(items);
    }

    private static String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        var normalized = reason.strip();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
