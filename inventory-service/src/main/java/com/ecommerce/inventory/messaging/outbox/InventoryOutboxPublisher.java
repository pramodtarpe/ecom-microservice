package com.ecommerce.inventory.messaging.outbox;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryOutboxPublisher {

    private final InventoryOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final Duration sendTimeout;

    public InventoryOutboxPublisher(
            InventoryOutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${messaging.outbox.batch-size:25}") int batchSize,
            @Value("${messaging.outbox.send-timeout:10s}") Duration sendTimeout) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.sendTimeout = sendTimeout;
    }

    @Scheduled(fixedDelayString = "${messaging.outbox.fixed-delay:500ms}")
    @Transactional
    public void publishPending() {
        var events = outboxRepository.findUnpublished(PageRequest.of(0, batchSize));
        for (var event : events) {
            publish(event);
            event.markPublished();
        }
    }

    private void publish(InventoryOutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing an outbox event", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not publish an outbox event", exception);
        }
    }
}
