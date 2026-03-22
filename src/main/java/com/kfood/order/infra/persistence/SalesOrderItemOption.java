package com.kfood.order.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sales_order_item_option")
public class SalesOrderItemOption extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_item_id", nullable = false)
  private SalesOrderItem orderItem;

  @Column(name = "option_name_snapshot", nullable = false, length = 255)
  private String optionNameSnapshot;

  @Column(name = "extra_price_snapshot", nullable = false, precision = 12, scale = 2)
  private BigDecimal extraPriceSnapshot;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  protected SalesOrderItemOption() {}

  private SalesOrderItemOption(
      UUID id, String optionNameSnapshot, BigDecimal extraPriceSnapshot, Integer quantity) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.optionNameSnapshot = normalizeRequired(optionNameSnapshot, "optionNameSnapshot");
    this.extraPriceSnapshot = normalizeMoney(extraPriceSnapshot, "extraPriceSnapshot");
    this.quantity = requirePositive(quantity, "quantity");
  }

  public static SalesOrderItemOption create(
      UUID id, String optionNameSnapshot, BigDecimal extraPriceSnapshot, Integer quantity) {
    return new SalesOrderItemOption(id, optionNameSnapshot, extraPriceSnapshot, quantity);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  void attachToOrderItem(SalesOrderItem orderItem) {
    this.orderItem = Objects.requireNonNull(orderItem, "orderItem must not be null");
  }

  public UUID getId() {
    return id;
  }

  public SalesOrderItem getOrderItem() {
    return orderItem;
  }

  public String getOptionNameSnapshot() {
    return optionNameSnapshot;
  }

  public BigDecimal getExtraPriceSnapshot() {
    return extraPriceSnapshot;
  }

  public Integer getQuantity() {
    return quantity;
  }

  @Transient
  public BigDecimal getTotalExtraAmount() {
    return extraPriceSnapshot
        .multiply(BigDecimal.valueOf(quantity))
        .setScale(2, RoundingMode.HALF_UP);
  }

  private void validateBusinessRules() {
    if (optionNameSnapshot.isBlank()) {
      throw new IllegalArgumentException("optionNameSnapshot must not be blank");
    }
    if (extraPriceSnapshot.signum() < 0) {
      throw new IllegalArgumentException("extraPriceSnapshot must not be negative");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be greater than zero");
    }
  }

  private static String normalizeRequired(String value, String fieldName) {
    var normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  private static BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
    var normalized = Objects.requireNonNull(value, fieldName + " must not be null");
    if (normalized.signum() < 0) {
      throw new IllegalArgumentException(fieldName + " must not be negative");
    }
    return normalized.setScale(2, RoundingMode.HALF_UP);
  }

  private static Integer requirePositive(Integer value, String fieldName) {
    var normalized = Objects.requireNonNull(value, fieldName + " must not be null");
    if (normalized <= 0) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
    return normalized;
  }
}
