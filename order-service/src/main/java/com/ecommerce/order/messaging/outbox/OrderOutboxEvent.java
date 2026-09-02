package com.ecommerce.order.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_outbox_events")
public class OrderOutboxEvent {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, updatable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, updatable = false, length = 100)
    private String messageKey;

    @Lob
    @Column(nullable = false, updatable = false)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OrderOutboxEvent() {
    }

    private OrderOutboxEvent(UUID id, String topic, String messageKey, String payload) {
        this.id = Objects.requireNonNull(id, "id").toString();
        this.topic = Objects.requireNonNull(topic, "topic");
        this.messageKey = Objects.requireNonNull(messageKey, "messageKey");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.createdAt = Instant.now();
    }

    public static OrderOutboxEvent pending(
            UUID id, String topic, String messageKey, String payload) {
        return new OrderOutboxEvent(id, topic, messageKey, payload);
    }

    public void markPublished() {
        if (publishedAt == null) {
            publishedAt = Instant.now();
        }
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }
}
