package com.ecommerce.inventory.messaging;

import com.ecommerce.events.inventory.InventoryCommandEvent;
import com.ecommerce.events.inventory.InventoryCommandType;
import com.ecommerce.events.inventory.InventoryLine;
import com.ecommerce.events.inventory.InventoryResultEvent;
import com.ecommerce.events.inventory.InventoryResultType;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.messaging.outbox.InventoryResultOutbox;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.reservation.InventoryReservation;
import com.ecommerce.inventory.reservation.InventoryReservationRepository;
import com.ecommerce.inventory.reservation.InventoryReservationStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCommandProcessor {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryResultOutbox resultOutbox;

    public InventoryCommandProcessor(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            InventoryResultOutbox resultOutbox) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.resultOutbox = resultOutbox;
    }

    @Transactional
    public void process(InventoryCommandEvent command) {
        var existing = reservationRepository.findByOrderIdForUpdate(command.orderId()).orElse(null);
        if (existing != null && existing.getLastSequence() >= command.sequence()) {
            return;
        }

        if (command.type() == InventoryCommandType.RESERVE) {
            requireSequence(command, 1);
            processReservation(command, existing);
        } else {
            requireSequence(command, 2);
            processRelease(command, existing);
        }
    }

    private void processReservation(
            InventoryCommandEvent command, InventoryReservation existing) {
        if (existing != null) {
            return;
        }

        var requested = aggregate(command.items());
        if (requested.failure() != null) {
            reject(command, requested.failure());
            return;
        }

        var lockedItems = new LinkedHashMap<String, InventoryItem>();
        for (var request : requested.quantities().entrySet()) {
            var item = inventoryRepository.findBySkuForUpdate(request.getKey()).orElse(null);
            if (item == null) {
                reject(command, new Failure(
                        "INVENTORY_NOT_FOUND",
                        "No inventory found for SKU " + request.getKey()));
                return;
            }
            if (item.getAvailableQuantity() < request.getValue()) {
                reject(command, new Failure(
                        "INSUFFICIENT_STOCK",
                        "Cannot reserve " + request.getValue() + " units of " + item.getSku()
                                + "; only " + item.getAvailableQuantity() + " units are available"));
                return;
            }
            if (Long.MAX_VALUE - item.getReservedQuantity() < request.getValue()) {
                reject(command, new Failure(
                        "QUANTITY_OVERFLOW",
                        "The reservation quantity is too large for SKU " + item.getSku()));
                return;
            }
            lockedItems.put(request.getKey(), item);
        }

        requested.quantities().forEach((sku, quantity) -> lockedItems.get(sku).reserve(quantity));
        inventoryRepository.saveAll(lockedItems.values());
        var reservation = reservationRepository.save(InventoryReservation.reserved(
                command.orderId(),
                command.sequence(),
                command.eventId(),
                toLines(requested.quantities())));
        resultOutbox.enqueue(resultFor(
                command,
                InventoryResultType.RESERVED,
                toLines(reservation),
                null));
    }

    private void processRelease(
            InventoryCommandEvent command, InventoryReservation existing) {
        InventoryReservation reservation = existing;
        if (reservation == null) {
            reservation = reservationRepository.save(InventoryReservation.releasedTombstone(
                    command.orderId(),
                    command.sequence(),
                    command.eventId(),
                    command.items()));
        } else {
            if (reservation.getStatus() == InventoryReservationStatus.RESERVED) {
                releaseReservedItems(reservation);
            }
            reservation.markReleased(command.sequence(), command.eventId());
        }

        resultOutbox.enqueue(resultFor(
                command,
                InventoryResultType.RELEASED,
                toLines(reservation),
                null));
    }

    private void releaseReservedItems(InventoryReservation reservation) {
        var lines = reservation.getItems().stream()
                .sorted((left, right) -> left.getSku().compareTo(right.getSku()))
                .toList();
        var lockedItems = new LinkedHashMap<String, InventoryItem>();
        for (var line : lines) {
            var item = inventoryRepository.findBySkuForUpdate(line.getSku())
                    .orElseThrow(() -> new IllegalStateException(
                            "Reserved inventory disappeared for SKU " + line.getSku()));
            if (item.getReservedQuantity() < line.getQuantity()) {
                throw new IllegalStateException(
                        "Reserved inventory is inconsistent for SKU " + line.getSku());
            }
            lockedItems.put(line.getSku(), item);
        }
        lines.forEach(line -> lockedItems.get(line.getSku()).release(line.getQuantity()));
        inventoryRepository.saveAll(lockedItems.values());
    }

    private void reject(InventoryCommandEvent command, Failure failure) {
        var reservation = reservationRepository.save(InventoryReservation.rejected(
                command.orderId(),
                command.sequence(),
                command.eventId(),
                command.items(),
                failure.code(),
                failure.reason()));
        resultOutbox.enqueue(resultFor(
                command,
                InventoryResultType.REJECTED,
                toLines(reservation),
                failure));
    }

    private AggregatedItems aggregate(List<InventoryLine> items) {
        var quantities = new java.util.TreeMap<String, Long>();
        try {
            for (var item : items) {
                quantities.merge(item.sku(), item.quantity(), Math::addExact);
            }
            return new AggregatedItems(quantities, null);
        } catch (ArithmeticException exception) {
            return new AggregatedItems(Map.of(), new Failure(
                    "QUANTITY_OVERFLOW", "The aggregate item quantity is too large"));
        }
    }

    private InventoryResultEvent resultFor(
            InventoryCommandEvent command,
            InventoryResultType type,
            List<InventoryLine> items,
            Failure failure) {
        return new InventoryResultEvent(
                UUID.randomUUID(),
                command.eventId(),
                InventoryResultEvent.CURRENT_SCHEMA_VERSION,
                type,
                Instant.now(),
                command.correlationId(),
                command.orderId(),
                command.sequence(),
                items,
                failure == null ? null : failure.code(),
                failure == null ? null : failure.reason());
    }

    private List<InventoryLine> toLines(Map<String, Long> quantities) {
        return quantities.entrySet().stream()
                .map(entry -> new InventoryLine(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<InventoryLine> toLines(InventoryReservation reservation) {
        return reservation.getItems().stream()
                .map(line -> new InventoryLine(line.getSku(), line.getQuantity()))
                .toList();
    }

    private void requireSequence(InventoryCommandEvent command, long expected) {
        if (command.sequence() != expected) {
            throw new IllegalArgumentException(
                    command.type() + " commands must use sequence " + expected);
        }
    }

    private record AggregatedItems(Map<String, Long> quantities, Failure failure) {
    }

    private record Failure(String code, String reason) {
    }
}
