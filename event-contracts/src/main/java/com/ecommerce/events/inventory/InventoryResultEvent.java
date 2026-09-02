package com.ecommerce.events.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A versioned inventory outcome correlated to one command event. */
public record InventoryResultEvent(
        UUID eventId,
        UUID causationId,
        int schemaVersion,
        InventoryResultType type,
        Instant occurredAt,
        String correlationId,
        long orderId,
        long sequence,
        List<InventoryLine> items,
        String reasonCode,
        String reason) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public InventoryResultEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(causationId, "causationId must not be null");
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

        reasonCode = normalizeOptionalText(reasonCode);
        reason = normalizeOptionalText(reason);
        if (type == InventoryResultType.REJECTED) {
            if (reasonCode == null || reason == null) {
                throw new IllegalArgumentException(
                        "a REJECTED result must contain reasonCode and reason");
            }
        } else if (reasonCode != null || reason != null) {
            throw new IllegalArgumentException(
                    "successful results must not contain reasonCode or reason");
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

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
