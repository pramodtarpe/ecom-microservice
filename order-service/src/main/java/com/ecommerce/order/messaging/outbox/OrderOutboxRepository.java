package com.ecommerce.order.messaging.outbox;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEvent, String> {

    @Query("""
            select event from OrderOutboxEvent event
            where event.publishedAt is null
            order by event.createdAt, event.id
            """)
    List<OrderOutboxEvent> findUnpublished(Pageable pageable);
}
