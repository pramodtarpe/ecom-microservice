package com.ecommerce.inventory.reservation;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("select reservation from InventoryReservation reservation where reservation.orderId = :orderId")
    Optional<InventoryReservation> findByOrderIdForUpdate(@Param("orderId") long orderId);
}
