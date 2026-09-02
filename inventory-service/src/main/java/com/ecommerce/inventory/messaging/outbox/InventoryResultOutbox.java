package com.ecommerce.inventory.messaging.outbox;

import com.ecommerce.events.inventory.InventoryResultEvent;
import com.ecommerce.events.inventory.InventoryTopics;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryResultOutbox {

    private final InventoryOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryResultOutbox(
            InventoryOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(InventoryResultEvent event) {
        try {
            var payload = objectMapper.writeValueAsString(event);
            outboxRepository.save(InventoryOutboxEvent.pending(
                    event.eventId(),
                    InventoryTopics.RESULTS,
                    Long.toString(event.orderId()),
                    payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize the inventory result", exception);
        }
    }
}
