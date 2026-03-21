package com.kfood.order.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

  Optional<SalesOrder> findByIdAndStoreId(UUID id, UUID storeId);

  Optional<SalesOrder> findByOrderNumber(String orderNumber);
}
