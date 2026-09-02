package com.ecommerce.order.service;

import com.ecommerce.order.client.CatalogClient;
import com.ecommerce.events.inventory.InventoryCommandEvent;
import com.ecommerce.events.inventory.InventoryCommandType;
import com.ecommerce.events.inventory.InventoryLine;
import com.ecommerce.events.inventory.InventoryResultEvent;
import com.ecommerce.events.inventory.InventoryResultType;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.PurchaseOrder;
import com.ecommerce.order.error.InvalidOrderRequestException;
import com.ecommerce.order.error.OrderNotFoundException;
import com.ecommerce.order.models.request.CreateOrderRequest;
import com.ecommerce.order.models.response.OrderResponse;
import com.ecommerce.order.messaging.outbox.InventoryCommandOutbox;
import com.ecommerce.order.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final PurchaseOrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final InventoryCommandOutbox inventoryCommandOutbox;

    public OrderService(
            PurchaseOrderRepository orderRepository,
            CatalogClient catalogClient,
            InventoryCommandOutbox inventoryCommandOutbox) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.inventoryCommandOutbox = inventoryCommandOutbox;
    }

    @Transactional
    public OrderResponse create(
            CreateOrderRequest request, String customerIdentity, String correlationId) {
        var quantities = aggregateItems(request.items());
        var prices = new LinkedHashMap<String, BigDecimal>();

        // Resolve every server-owned price before creating any inventory side effect.
        quantities.keySet().forEach(sku -> prices.put(sku, catalogClient.priceForSku(sku)));

        var order = new PurchaseOrder(customerIdentity);
        quantities.forEach((sku, quantity) -> order.addItem(sku, quantity, prices.get(sku)));
        orderRepository.saveAndFlush(order);
        inventoryCommandOutbox.enqueue(commandFor(
                order, InventoryCommandType.RESERVE, correlationId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(String customerIdentity, boolean admin) {
        var orders = admin
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findAllByCustomerEmailOrderByCreatedAtDesc(customerIdentity);
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(long id, String customerIdentity, boolean admin) {
        return OrderResponse.from(requireOrder(id, customerIdentity, admin));
    }

    @Transactional
    public OrderResponse cancel(
            long id, String customerIdentity, boolean admin, String correlationId) {
        var order = requireOrderForUpdate(id, customerIdentity, admin);
        if (!order.requestCancellation()) {
            return OrderResponse.from(order);
        }
        orderRepository.saveAndFlush(order);
        inventoryCommandOutbox.enqueue(commandFor(
                order, InventoryCommandType.RELEASE, correlationId));
        return OrderResponse.from(order);
    }

    @Transactional
    public void applyInventoryResult(InventoryResultEvent event) {
        var order = orderRepository.findByIdForUpdate(event.orderId()).orElse(null);
        if (order == null) {
            return;
        }

        if (event.type() == InventoryResultType.RESERVED
                && event.sequence() == 1
                && order.getStatus() == OrderStatus.PENDING_INVENTORY) {
            order.confirmInventory();
        } else if (event.type() == InventoryResultType.REJECTED
                && event.sequence() == 1
                && order.getStatus() == OrderStatus.PENDING_INVENTORY) {
            order.rejectInventory(event.reason());
        } else if (event.type() == InventoryResultType.RELEASED
                && event.sequence() == order.getInventorySequence()
                && order.getStatus() == OrderStatus.CANCELLATION_PENDING) {
            order.completeCancellation();
        }
    }

    private PurchaseOrder requireOrder(long id, String customerIdentity, boolean admin) {
        var order = admin
                ? orderRepository.findById(id)
                : orderRepository.findByIdAndCustomerEmail(id, customerIdentity);
        return order.orElseThrow(() -> new OrderNotFoundException(id));
    }

    private PurchaseOrder requireOrderForUpdate(long id, String customerIdentity, boolean admin) {
        var order = admin
                ? orderRepository.findByIdForUpdate(id)
                : orderRepository.findByIdAndCustomerEmailForUpdate(id, customerIdentity);
        return order.orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Map<String, Integer> aggregateItems(List<CreateOrderRequest.Item> items) {
        var quantities = new LinkedHashMap<String, Integer>();
        try {
            items.forEach(item -> quantities.merge(
                    normalizeSku(item.sku()), item.quantity(), Math::addExact));
        } catch (ArithmeticException exception) {
            throw new InvalidOrderRequestException("The aggregate item quantity is too large", exception);
        }
        return quantities;
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private InventoryCommandEvent commandFor(
            PurchaseOrder order, InventoryCommandType type, String correlationId) {
        var items = order.getItems().stream()
                .map(item -> new InventoryLine(item.getSku(), item.getQuantity()))
                .toList();
        return new InventoryCommandEvent(
                UUID.randomUUID(),
                InventoryCommandEvent.CURRENT_SCHEMA_VERSION,
                type,
                Instant.now(),
                correlationId,
                order.getId(),
                order.getInventorySequence(),
                items);
    }
}
