package com.kfood.order.infra.persistence;

import com.kfood.order.domain.OrderStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {

  Optional<SalesOrder> findByIdAndStoreId(UUID id, UUID storeId);

  Optional<SalesOrder> findByOrderNumber(String orderNumber);

  @Query(
      """
      select o
      from SalesOrder o
      where o.store.id = :storeId
        and o.status = :status
        and (o.scheduledFor is null or o.scheduledFor <= :referenceTime)
      order by o.createdAt desc
      """)
  Page<SalesOrder> findOperationalQueueByStoreIdAndStatus(
      @Param("storeId") UUID storeId,
      @Param("status") OrderStatus status,
      @Param("referenceTime") OffsetDateTime referenceTime,
      Pageable pageable);
}
