package com.kfood.order.infra.persistence;

import com.kfood.customer.infra.persistence.Customer;
import com.kfood.merchant.infra.persistence.Store;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sales_order")
public class SalesOrder extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "order_number", length = 50)
  private String orderNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "fulfillment_type", nullable = false, length = 20)
  private FulfillmentType fulfillmentType;

  @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotalAmount;

  @Column(name = "delivery_fee_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal deliveryFeeAmount;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "scheduled_for")
  private OffsetDateTime scheduledFor;

  @Column(name = "notes", length = 1000)
  private String notes;

  protected SalesOrder() {}

  private SalesOrder(
      UUID id,
      Store store,
      Customer customer,
      FulfillmentType fulfillmentType,
      BigDecimal subtotalAmount,
      BigDecimal deliveryFeeAmount,
      BigDecimal totalAmount,
      OffsetDateTime scheduledFor,
      String notes) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.store = Objects.requireNonNull(store, "store must not be null");
    this.customer = Objects.requireNonNull(customer, "customer must not be null");
    this.fulfillmentType =
        Objects.requireNonNull(fulfillmentType, "fulfillmentType must not be null");
    this.subtotalAmount = normalizeMoney(subtotalAmount, "subtotalAmount");
    this.deliveryFeeAmount = normalizeMoney(deliveryFeeAmount, "deliveryFeeAmount");
    this.totalAmount = normalizeMoney(totalAmount, "totalAmount");
    this.scheduledFor = scheduledFor;
    this.notes = normalizeNullable(notes);
    status = OrderStatus.NEW;
    validateBusinessRules();
  }

  public static SalesOrder create(
      UUID id,
      Store store,
      Customer customer,
      FulfillmentType fulfillmentType,
      BigDecimal subtotalAmount,
      BigDecimal deliveryFeeAmount,
      BigDecimal totalAmount,
      OffsetDateTime scheduledFor,
      String notes) {
    return new SalesOrder(
        id,
        store,
        customer,
        fulfillmentType,
        subtotalAmount,
        deliveryFeeAmount,
        totalAmount,
        scheduledFor,
        notes);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  public UUID getId() {
    return id;
  }

  public Store getStore() {
    return store;
  }

  public Customer getCustomer() {
    return customer;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public FulfillmentType getFulfillmentType() {
    return fulfillmentType;
  }

  public BigDecimal getSubtotalAmount() {
    return subtotalAmount;
  }

  public BigDecimal getDeliveryFeeAmount() {
    return deliveryFeeAmount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public OffsetDateTime getScheduledFor() {
    return scheduledFor;
  }

  public String getNotes() {
    return notes;
  }

  public void assignOrderNumber(String orderNumber) {
    if (orderNumber == null || orderNumber.isBlank()) {
      throw new IllegalArgumentException("orderNumber must not be blank");
    }
    this.orderNumber = orderNumber.trim();
  }

  private void validateBusinessRules() {
    if (subtotalAmount.signum() < 0) {
      throw new IllegalArgumentException("subtotalAmount must not be negative");
    }
    if (deliveryFeeAmount.signum() < 0) {
      throw new IllegalArgumentException("deliveryFeeAmount must not be negative");
    }
    if (totalAmount.signum() < 0) {
      throw new IllegalArgumentException("totalAmount must not be negative");
    }

    var expectedTotal = subtotalAmount.add(deliveryFeeAmount).setScale(2, RoundingMode.HALF_UP);
    if (expectedTotal.compareTo(totalAmount) != 0) {
      throw new IllegalArgumentException(
          "totalAmount must be equal to subtotalAmount + deliveryFeeAmount");
    }
  }

  private static BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
    var normalized = Objects.requireNonNull(value, fieldName + " must not be null");
    return normalized.setScale(2, RoundingMode.HALF_UP);
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
