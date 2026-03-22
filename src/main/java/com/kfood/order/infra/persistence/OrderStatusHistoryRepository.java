package com.kfood.order.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

  List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(UUID orderId);
}
