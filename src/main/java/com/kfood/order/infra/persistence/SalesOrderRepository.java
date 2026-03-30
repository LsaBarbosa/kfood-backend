package com.kfood.order.infra.persistence;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.app.port.PaymentOrder;
import com.kfood.payment.app.port.PaymentOrderLookupPort;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SalesOrderRepository
    extends JpaRepository<SalesOrder, UUID>,
        JpaSpecificationExecutor<SalesOrder>,
        PaymentOrderLookupPort {

  @Override
  default Optional<PaymentOrder> findOrderById(UUID orderId) {
    return findById(orderId).map(order -> order);
  }

  @Override
  default Optional<PaymentOrder> findOrderByIdAndStoreId(UUID orderId, UUID storeId) {
    return findByIdAndStore_Id(orderId, storeId).map(order -> order);
  }

  Optional<SalesOrder> findByIdAndStore_Id(UUID id, UUID storeId);

  @EntityGraph(attributePaths = {"customer", "items"})
  Optional<SalesOrder> findDetailedByIdAndStore_Id(UUID id, UUID storeId);

  Optional<SalesOrder> findByOrderNumber(String orderNumber);

  Optional<SalesOrder> findByStore_IdAndOrderNumber(UUID storeId, String orderNumber);

  @Override
  @EntityGraph(attributePaths = "customer")
  Page<SalesOrder> findAll(Specification<SalesOrder> spec, Pageable pageable);

  default Page<SalesOrder> findOperationalQueue(
      UUID storeId,
      OrderStatus status,
      FulfillmentType fulfillmentType,
      Instant createdFrom,
      Instant createdToExclusive,
      OffsetDateTime referenceTime,
      Pageable pageable) {
    var specifications = new ArrayList<Specification<SalesOrder>>();
    specifications.add(byStoreId(storeId));
    specifications.add(availableForOperationAt(referenceTime));

    var statusSpecification = byStatus(status);
    if (statusSpecification != null) {
      specifications.add(statusSpecification);
    }

    var fulfillmentTypeSpecification = byFulfillmentType(fulfillmentType);
    if (fulfillmentTypeSpecification != null) {
      specifications.add(fulfillmentTypeSpecification);
    }

    var createdFromSpecification = createdAtGreaterThanOrEqualTo(createdFrom);
    if (createdFromSpecification != null) {
      specifications.add(createdFromSpecification);
    }

    var createdToExclusiveSpecification = createdAtLessThan(createdToExclusive);
    if (createdToExclusiveSpecification != null) {
      specifications.add(createdToExclusiveSpecification);
    }

    return findAll(Specification.allOf(specifications), pageable);
  }

  private static Specification<SalesOrder> byStoreId(UUID storeId) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("store").get("id"), storeId);
  }

  private static Specification<SalesOrder> byStatus(OrderStatus status) {
    return status == null
        ? null
        : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
  }

  private static Specification<SalesOrder> byFulfillmentType(FulfillmentType fulfillmentType) {
    return fulfillmentType == null
        ? null
        : (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("fulfillmentType"), fulfillmentType);
  }

  private static Specification<SalesOrder> createdAtGreaterThanOrEqualTo(Instant createdFrom) {
    return createdFrom == null
        ? null
        : (root, query, criteriaBuilder) ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
  }

  private static Specification<SalesOrder> createdAtLessThan(Instant createdToExclusive) {
    return createdToExclusive == null
        ? null
        : (root, query, criteriaBuilder) ->
            criteriaBuilder.lessThan(root.get("createdAt"), createdToExclusive);
  }

  private static Specification<SalesOrder> availableForOperationAt(OffsetDateTime referenceTime) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.or(
            criteriaBuilder.isNull(root.get("scheduledFor")),
            criteriaBuilder.lessThanOrEqualTo(root.get("scheduledFor"), referenceTime));
  }
}
