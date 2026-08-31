package com.ecommerce.order.service;

import com.ecommerce.order.api.CreateOrderRequest;
import com.ecommerce.order.api.OrderResponse;
import com.ecommerce.order.client.CatalogClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.PurchaseOrder;
import com.ecommerce.order.error.InvalidOrderRequestException;
import com.ecommerce.order.error.OrderNotFoundException;
import com.ecommerce.order.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final PurchaseOrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;

    public OrderService(
            PurchaseOrderRepository orderRepository,
            CatalogClient catalogClient,
            InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String customerIdentity) {
        var quantities = aggregateItems(request.items());
        var prices = new LinkedHashMap<String, BigDecimal>();

        // Resolve every server-owned price before creating any inventory side effect.
        quantities.keySet().forEach(sku -> prices.put(sku, catalogClient.priceForSku(sku)));

        var completedReservations = new ArrayList<Reservation>();
        try {
            quantities.forEach((sku, quantity) -> {
                inventoryClient.reserve(sku, quantity);
                completedReservations.add(new Reservation(sku, quantity));
            });

            var order = new PurchaseOrder(customerIdentity);
            quantities.forEach((sku, quantity) -> order.addItem(sku, quantity, prices.get(sku)));
            return OrderResponse.from(orderRepository.saveAndFlush(order));
        } catch (RuntimeException failure) {
            compensateReservations(completedReservations, failure);
            throw failure;
        }
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
    public OrderResponse cancel(long id, String customerIdentity, boolean admin) {
        // Serialize cancellation for one order so concurrent callers cannot release
        // the same inventory reservation more than once.
        var order = requireOrderForUpdate(id, customerIdentity, admin);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order);
        }

        releaseAll(order);
        order.cancel();
        try {
            return OrderResponse.from(orderRepository.saveAndFlush(order));
        } catch (RuntimeException persistenceFailure) {
            restoreReservations(order, persistenceFailure);
            throw persistenceFailure;
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

    private void compensateReservations(List<Reservation> reservations, RuntimeException originalFailure) {
        for (int index = reservations.size() - 1; index >= 0; index--) {
            var reservation = reservations.get(index);
            try {
                inventoryClient.release(reservation.sku(), reservation.quantity());
            } catch (RuntimeException compensationFailure) {
                originalFailure.addSuppressed(compensationFailure);
            }
        }
    }

    private void releaseAll(PurchaseOrder order) {
        RuntimeException firstFailure = null;
        var completedReleases = new ArrayList<Reservation>();
        for (var item : order.getItems()) {
            try {
                inventoryClient.release(item.getSku(), item.getQuantity());
                completedReleases.add(new Reservation(item.getSku(), item.getQuantity()));
            } catch (RuntimeException releaseFailure) {
                if (firstFailure == null) {
                    firstFailure = releaseFailure;
                } else {
                    firstFailure.addSuppressed(releaseFailure);
                }
            }
        }
        if (firstFailure != null) {
            restoreReservations(completedReleases, firstFailure);
            throw firstFailure;
        }
    }

    private void restoreReservations(PurchaseOrder order, RuntimeException persistenceFailure) {
        restoreReservations(order.getItems().stream()
                .map(item -> new Reservation(item.getSku(), item.getQuantity()))
                .toList(), persistenceFailure);
    }

    private void restoreReservations(List<Reservation> reservations, RuntimeException originalFailure) {
        for (int index = reservations.size() - 1; index >= 0; index--) {
            var reservation = reservations.get(index);
            try {
                inventoryClient.reserve(reservation.sku(), reservation.quantity());
            } catch (RuntimeException restoreFailure) {
                originalFailure.addSuppressed(restoreFailure);
            }
        }
    }

    private record Reservation(String sku, int quantity) {
    }
}
