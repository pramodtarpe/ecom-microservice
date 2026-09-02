package com.ecommerce.inventory.messaging;

import com.ecommerce.events.inventory.InventoryCommandEvent;
import com.ecommerce.events.inventory.InventoryTopics;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryCommandListener {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final ObjectMapper objectMapper;
    private final InventoryCommandProcessor commandProcessor;

    public InventoryCommandListener(
            ObjectMapper objectMapper, InventoryCommandProcessor commandProcessor) {
        this.objectMapper = objectMapper;
        this.commandProcessor = commandProcessor;
    }

    @KafkaListener(topics = InventoryTopics.COMMANDS)
    public void onInventoryCommand(String payload) {
        InventoryCommandEvent event;
        try {
            event = objectMapper.readValue(payload, InventoryCommandEvent.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid inventory command event", exception);
        }

        String previousCorrelationId = MDC.get(CORRELATION_ID_MDC_KEY);
        MDC.put(CORRELATION_ID_MDC_KEY, event.correlationId());
        try {
            commandProcessor.process(event);
        } finally {
            if (previousCorrelationId == null) {
                MDC.remove(CORRELATION_ID_MDC_KEY);
            } else {
                MDC.put(CORRELATION_ID_MDC_KEY, previousCorrelationId);
            }
        }
    }
}
