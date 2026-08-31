package com.ecommerce.order.repository;

import com.ecommerce.order.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = "items")
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "items")
    List<PurchaseOrder> findAllByCustomerEmailOrderByCreatedAtDesc(String customerEmail);

    @EntityGraph(attributePaths = "items")
    Optional<PurchaseOrder> findByIdAndCustomerEmail(long id, String customerEmail);
}
