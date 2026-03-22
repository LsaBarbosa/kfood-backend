package com.kfood.order.infra.persistence;

import com.kfood.order.domain.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sales_order_status_history")
public class OrderStatusHistory {

  @Id private UUID id;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", nullable = false, length = 30)
  private OrderStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 30)
  private OrderStatus newStatus;

  @Column(name = "actor_user_id", nullable = false)
  private UUID actorUserId;

  @Column(name = "reason", length = 255)
  private String reason;

  @Column(name = "changed_at", nullable = false, updatable = false)
  private Instant changedAt;

  protected OrderStatusHistory() {}

  private OrderStatusHistory(
      UUID id,
      UUID storeId,
      UUID orderId,
      OrderStatus previousStatus,
      OrderStatus newStatus,
      UUID actorUserId,
      Instant changedAt,
      String reason) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.storeId = Objects.requireNonNull(storeId, "storeId is required");
    this.orderId = Objects.requireNonNull(orderId, "orderId is required");
    this.previousStatus = Objects.requireNonNull(previousStatus, "previousStatus is required");
    this.newStatus = Objects.requireNonNull(newStatus, "newStatus is required");
    this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId is required");
    this.changedAt = Objects.requireNonNull(changedAt, "changedAt is required");
    this.reason = normalizeNullable(reason);

    if (previousStatus == newStatus) {
      throw new IllegalArgumentException("previousStatus must differ from newStatus");
    }
  }

  public static OrderStatusHistory create(
      UUID id,
      UUID storeId,
      UUID orderId,
      OrderStatus previousStatus,
      OrderStatus newStatus,
      UUID actorUserId,
      Instant changedAt,
      String reason) {
    return new OrderStatusHistory(
        id, storeId, orderId, previousStatus, newStatus, actorUserId, changedAt, reason);
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public OrderStatus getPreviousStatus() {
    return previousStatus;
  }

  public OrderStatus getNewStatus() {
    return newStatus;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public String getReason() {
    return reason;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
