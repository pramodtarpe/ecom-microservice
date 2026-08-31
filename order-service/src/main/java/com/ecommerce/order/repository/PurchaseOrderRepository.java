package com.ecommerce.order.repository;

import com.ecommerce.order.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = "items")
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<PurchaseOrder> findAllByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByIdAndCustomerEmail(long id, String customerEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder where purchaseOrder.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("""
            select purchaseOrder from PurchaseOrder purchaseOrder
            where purchaseOrder.id = :id and purchaseOrder.customerEmail = :customerIdentity
            """)
    Optional<PurchaseOrder> findByIdAndCustomerEmailForUpdate(
            @Param("id") long id,
            @Param("customerIdentity") String customerIdentity);
}
