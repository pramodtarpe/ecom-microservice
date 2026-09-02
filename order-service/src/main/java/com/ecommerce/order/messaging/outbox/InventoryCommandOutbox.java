package com.ecommerce.order.messaging.outbox;

import com.ecommerce.events.inventory.InventoryCommandEvent;
import com.ecommerce.events.inventory.InventoryTopics;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryCommandOutbox {

    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryCommandOutbox(
            OrderOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(InventoryCommandEvent event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(OrderOutboxEvent.pending(
                    event.eventId(),
                    InventoryTopics.COMMANDS,
                    Long.toString(event.orderId()),
                    payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize the inventory command", exception);
        }
    }
}
