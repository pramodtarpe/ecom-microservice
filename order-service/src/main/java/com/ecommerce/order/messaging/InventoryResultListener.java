package com.ecommerce.order.messaging;

import com.ecommerce.events.inventory.InventoryResultEvent;
import com.ecommerce.events.inventory.InventoryTopics;
import com.ecommerce.order.service.OrderService;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryResultListener {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public InventoryResultListener(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = InventoryTopics.RESULTS)
    public void onInventoryResult(String payload) {
        InventoryResultEvent event;
        try {
            event = objectMapper.readValue(payload, InventoryResultEvent.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid inventory result event", exception);
        }

        String previousCorrelationId = MDC.get(CORRELATION_ID_MDC_KEY);
        MDC.put(CORRELATION_ID_MDC_KEY, event.correlationId());
        try {
            orderService.applyInventoryResult(event);
        } finally {
            if (previousCorrelationId == null) {
                MDC.remove(CORRELATION_ID_MDC_KEY);
            } else {
                MDC.put(CORRELATION_ID_MDC_KEY, previousCorrelationId);
            }
        }
    }
}
