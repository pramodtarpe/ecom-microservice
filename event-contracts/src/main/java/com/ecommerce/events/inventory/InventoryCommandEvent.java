package com.ecommerce.events.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A versioned request to reserve or release inventory for an order. */
public record InventoryCommandEvent(
        UUID eventId,
        int schemaVersion,
        InventoryCommandType type,
        Instant occurredAt,
        String correlationId,
        long orderId,
        long sequence,
        List<InventoryLine> items) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public InventoryCommandEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        requireCurrentSchema(schemaVersion);
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        correlationId = requireCorrelationId(correlationId);
        requirePositive(orderId, "orderId");
        requirePositive(sequence, "sequence");
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("items must not contain null elements");
        }
        if (type == InventoryCommandType.RESERVE && items.isEmpty()) {
            throw new IllegalArgumentException("a RESERVE command must contain at least one item");
        }
    }

    private static void requireCurrentSchema(int schemaVersion) {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "schemaVersion must be " + CURRENT_SCHEMA_VERSION);
        }
    }

    private static String requireCorrelationId(String correlationId) {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        if (correlationId.isBlank() || correlationId.length() > 64) {
            throw new IllegalArgumentException(
                    "correlationId must contain between 1 and 64 characters");
        }
        return correlationId;
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
