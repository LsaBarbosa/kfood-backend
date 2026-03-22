package com.kfood.payment.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  @EntityGraph(attributePaths = {"order", "order.store"})
  Optional<Payment> findByIdAndOrderStoreId(UUID id, UUID storeId);

  List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
