package com.ecommerce.inventory.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryCommandProcessorTest {

    private static final long ORDER_ID = 42L;
    private static final String CORRELATION_ID = "inventory-command-test";

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private InventoryResultOutbox resultOutbox;

    private InventoryCommandProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new InventoryCommandProcessor(
                inventoryRepository, reservationRepository, resultOutbox);
    }

    @Test
    void duplicateReserveDoesNotMutateStockOrEmitASecondResult() {
        var stock = InventoryItem.create("SKU-LAPTOP", 10);
        var command = command(
                InventoryCommandType.RESERVE,
                1,
                List.of(new InventoryLine("SKU-LAPTOP", 3)));
        var storedReservation = new AtomicReference<InventoryReservation>();

        when(reservationRepository.findByOrderIdForUpdate(ORDER_ID))
                .thenAnswer(ignored -> Optional.ofNullable(storedReservation.get()));
        when(inventoryRepository.findBySkuForUpdate("SKU-LAPTOP"))
                .thenReturn(Optional.of(stock));
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> {
                    var reservation = invocation.getArgument(0, InventoryReservation.class);
                    storedReservation.set(reservation);
                    return reservation;
                });

        processor.process(command);
        processor.process(command);

        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
        assertThat(storedReservation.get().getStatus())
                .isEqualTo(InventoryReservationStatus.RESERVED);
        assertThat(storedReservation.get().getLastSequence()).isEqualTo(1);
        verify(inventoryRepository).findBySkuForUpdate("SKU-LAPTOP");
        verify(inventoryRepository).saveAll(any());
        verify(reservationRepository).save(any(InventoryReservation.class));
        verify(resultOutbox).enqueue(any(InventoryResultEvent.class));
    }

    @Test
    void insufficientStockRejectsTheWholeReservationWithoutMutatingAnySku() {
        var laptop = InventoryItem.create("SKU-LAPTOP", 10);
        var mouse = InventoryItem.create("SKU-MOUSE", 1);
        var command = command(
                InventoryCommandType.RESERVE,
                1,
                List.of(
                        new InventoryLine("SKU-LAPTOP", 3),
                        new InventoryLine("SKU-MOUSE", 2)));

        when(reservationRepository.findByOrderIdForUpdate(ORDER_ID))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findBySkuForUpdate("SKU-LAPTOP"))
                .thenReturn(Optional.of(laptop));
        when(inventoryRepository.findBySkuForUpdate("SKU-MOUSE"))
                .thenReturn(Optional.of(mouse));
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var resultCaptor = ArgumentCaptor.forClass(InventoryResultEvent.class);

        processor.process(command);

        assertThat(laptop.getAvailableQuantity()).isEqualTo(10);
        assertThat(laptop.getReservedQuantity()).isZero();
        assertThat(mouse.getAvailableQuantity()).isEqualTo(1);
        assertThat(mouse.getReservedQuantity()).isZero();
        verify(inventoryRepository, never()).saveAll(any());
        verify(resultOutbox).enqueue(resultCaptor.capture());
        assertThat(resultCaptor.getValue().type()).isEqualTo(InventoryResultType.REJECTED);
        assertThat(resultCaptor.getValue().reasonCode()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(resultCaptor.getValue().items())
                .containsExactly(
                        new InventoryLine("SKU-LAPTOP", 3),
                        new InventoryLine("SKU-MOUSE", 2));
    }

    @Test
    void releaseRestoresReservedStockAndDuplicateReleaseDoesNothing() {
        var stock = InventoryItem.create("SKU-LAPTOP", 10);
        stock.reserve(3);
        var reservation = InventoryReservation.reserved(
                ORDER_ID,
                1,
                UUID.randomUUID(),
                List.of(new InventoryLine("SKU-LAPTOP", 3)));
        var release = command(
                InventoryCommandType.RELEASE,
                2,
                List.of(new InventoryLine("SKU-LAPTOP", 3)));

        when(reservationRepository.findByOrderIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.findBySkuForUpdate("SKU-LAPTOP"))
                .thenReturn(Optional.of(stock));
        var resultCaptor = ArgumentCaptor.forClass(InventoryResultEvent.class);

        processor.process(release);
        processor.process(release);

        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        assertThat(stock.getReservedQuantity()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(reservation.getLastSequence()).isEqualTo(2);
        verify(inventoryRepository).findBySkuForUpdate("SKU-LAPTOP");
        verify(inventoryRepository).saveAll(any());
        verify(resultOutbox, times(1)).enqueue(resultCaptor.capture());
        assertThat(resultCaptor.getValue().type()).isEqualTo(InventoryResultType.RELEASED);
        assertThat(resultCaptor.getValue().sequence()).isEqualTo(2);
    }

    private InventoryCommandEvent command(
            InventoryCommandType type, long sequence, List<InventoryLine> items) {
        return new InventoryCommandEvent(
                UUID.randomUUID(),
                InventoryCommandEvent.CURRENT_SCHEMA_VERSION,
                type,
                Instant.now(),
                CORRELATION_ID,
                ORDER_ID,
                sequence,
                items);
    }
}
