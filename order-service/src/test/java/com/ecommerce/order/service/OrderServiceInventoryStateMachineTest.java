package com.ecommerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.events.inventory.InventoryCommandEvent;
import com.ecommerce.events.inventory.InventoryCommandType;
import com.ecommerce.events.inventory.InventoryLine;
import com.ecommerce.events.inventory.InventoryResultEvent;
import com.ecommerce.events.inventory.InventoryResultType;
import com.ecommerce.order.client.CatalogClient;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.PurchaseOrder;
import com.ecommerce.order.messaging.outbox.InventoryCommandOutbox;
import com.ecommerce.order.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceInventoryStateMachineTest {

    private static final long ORDER_ID = 42L;
    private static final String CUSTOMER = "buyer@example.com";
    private static final String CORRELATION_ID = "order-state-test";

    @Mock
    private PurchaseOrderRepository orderRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private InventoryCommandOutbox inventoryCommandOutbox;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, catalogClient, inventoryCommandOutbox);
    }

    @Test
    void reservedResultConfirmsOnlyAPendingOrder() {
        var order = pendingOrder();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.applyInventoryResult(result(InventoryResultType.RESERVED, 1, null));
        orderService.applyInventoryResult(result(
                InventoryResultType.REJECTED, 1, "A late duplicate rejection"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getStatusReason()).isNull();
        verify(orderRepository, times(2)).findByIdForUpdate(ORDER_ID);
    }

    @Test
    void rejectedResultMovesPendingOrderToRejectedAndKeepsTheReason() {
        var order = pendingOrder();
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.applyInventoryResult(result(
                InventoryResultType.REJECTED,
                1,
                "  Not enough stock for SKU-LAPTOP  "));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getStatusReason()).isEqualTo("Not enough stock for SKU-LAPTOP");
    }

    @Test
    void cancellationPublishesSequenceTwoOnceAndWaitsForMatchingRelease() {
        var order = pendingOrder();
        when(orderRepository.findByIdAndCustomerEmailForUpdate(ORDER_ID, CUSTOMER))
                .thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);
        var commandCaptor = ArgumentCaptor.forClass(InventoryCommandEvent.class);

        var firstResponse = orderService.cancel(
                ORDER_ID, CUSTOMER, false, CORRELATION_ID);
        var duplicateResponse = orderService.cancel(
                ORDER_ID, CUSTOMER, false, CORRELATION_ID);

        assertThat(firstResponse.status()).isEqualTo(OrderStatus.CANCELLATION_PENDING);
        assertThat(duplicateResponse.status()).isEqualTo(OrderStatus.CANCELLATION_PENDING);
        assertThat(order.getInventorySequence()).isEqualTo(2);
        verify(inventoryCommandOutbox).enqueue(commandCaptor.capture());
        assertThat(commandCaptor.getValue().type()).isEqualTo(InventoryCommandType.RELEASE);
        assertThat(commandCaptor.getValue().sequence()).isEqualTo(2);
        assertThat(commandCaptor.getValue().orderId()).isEqualTo(ORDER_ID);
        assertThat(commandCaptor.getValue().correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(commandCaptor.getValue().items())
                .containsExactly(new InventoryLine("SKU-LAPTOP", 2));

        orderService.applyInventoryResult(result(InventoryResultType.RESERVED, 1, null));
        orderService.applyInventoryResult(result(InventoryResultType.RELEASED, 1, null));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLATION_PENDING);

        orderService.applyInventoryResult(result(InventoryResultType.RELEASED, 2, null));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        var alreadyCancelled = orderService.cancel(
                ORDER_ID, CUSTOMER, false, CORRELATION_ID);
        assertThat(alreadyCancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryCommandOutbox, times(1)).enqueue(commandCaptor.capture());
        verify(orderRepository, times(1)).saveAndFlush(order);
    }

    private PurchaseOrder pendingOrder() {
        var order = new PurchaseOrder(CUSTOMER);
        order.addItem("SKU-LAPTOP", 2, new BigDecimal("1250.00"));
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }

    private InventoryResultEvent result(
            InventoryResultType type, long sequence, String reason) {
        return new InventoryResultEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                InventoryResultEvent.CURRENT_SCHEMA_VERSION,
                type,
                Instant.now(),
                CORRELATION_ID,
                ORDER_ID,
                sequence,
                List.of(new InventoryLine("SKU-LAPTOP", 2)),
                type == InventoryResultType.REJECTED ? "INVENTORY_REJECTED" : null,
                type == InventoryResultType.REJECTED ? reason : null);
    }
}
