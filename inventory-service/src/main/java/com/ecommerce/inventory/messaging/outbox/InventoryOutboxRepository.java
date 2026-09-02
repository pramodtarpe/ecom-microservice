package com.ecommerce.inventory.messaging.outbox;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryOutboxRepository extends JpaRepository<InventoryOutboxEvent, String> {

    @Query("""
            select event from InventoryOutboxEvent event
            where event.publishedAt is null
            order by event.createdAt, event.id
            """)
    List<InventoryOutboxEvent> findUnpublished(Pageable pageable);
}
