package com.kfood.order.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "sales_order_item")
public class SalesOrderItem extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private SalesOrder order;

  @Column(name = "product_id")
  private UUID productId;

  @Column(name = "product_name_snapshot", nullable = false, length = 255)
  private String productNameSnapshot;

  @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPriceSnapshot;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "total_item_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalItemAmount;

  @Column(name = "notes", length = 1000)
  private String notes;

  @OneToMany(
      mappedBy = "orderItem",
      cascade = jakarta.persistence.CascadeType.ALL,
      orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  private final List<SalesOrderItemOption> options = new ArrayList<>();

  protected SalesOrderItem() {}

  private SalesOrderItem(
      UUID id,
      UUID productId,
      String productNameSnapshot,
      BigDecimal unitPriceSnapshot,
      Integer quantity,
      String notes) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.productId = Objects.requireNonNull(productId, "productId must not be null");
    this.productNameSnapshot = normalizeRequired(productNameSnapshot, "productNameSnapshot");
    this.unitPriceSnapshot = normalizeMoney(unitPriceSnapshot, "unitPriceSnapshot");
    this.quantity = requirePositive(quantity, "quantity");
    this.notes = normalizeNullable(notes);
    totalItemAmount = calculateTotal();
  }

  public static SalesOrderItem create(
      UUID id,
      UUID productId,
      String productNameSnapshot,
      BigDecimal unitPriceSnapshot,
      Integer quantity,
      String notes) {
    return new SalesOrderItem(
        id, productId, productNameSnapshot, unitPriceSnapshot, quantity, notes);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  void attachToOrder(SalesOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  public void addOption(SalesOrderItemOption option) {
    var validatedOption = Objects.requireNonNull(option, "option must not be null");
    validatedOption.attachToOrderItem(this);
    options.add(validatedOption);
    totalItemAmount = calculateTotal();
  }

  public UUID getId() {
    return id;
  }

  public SalesOrder getOrder() {
    return order;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getProductNameSnapshot() {
    return productNameSnapshot;
  }

  public BigDecimal getUnitPriceSnapshot() {
    return unitPriceSnapshot;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public BigDecimal getTotalItemAmount() {
    return totalItemAmount;
  }

  public String getNotes() {
    return notes;
  }

  public List<SalesOrderItemOption> getOptions() {
    return Collections.unmodifiableList(options);
  }

  @Transient
  public BigDecimal getOptionExtrasPerUnit() {
    return options.stream()
        .map(SalesOrderItemOption::getTotalExtraAmount)
        .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  private void validateBusinessRules() {
    if (productId == null) {
      throw new IllegalArgumentException("productId must not be null");
    }
    if (productNameSnapshot.isBlank()) {
      throw new IllegalArgumentException("productNameSnapshot must not be blank");
    }
    if (unitPriceSnapshot.signum() < 0) {
      throw new IllegalArgumentException("unitPriceSnapshot must not be negative");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be greater than zero");
    }
    if (totalItemAmount.signum() < 0) {
      throw new IllegalArgumentException("totalItemAmount must not be negative");
    }
  }

  private BigDecimal calculateTotal() {
    var baseTotal =
        unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    var extrasPerUnit =
        options.stream()
            .map(SalesOrderItemOption::getTotalExtraAmount)
            .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    return baseTotal
        .add(extrasPerUnit.multiply(BigDecimal.valueOf(quantity)))
        .setScale(2, RoundingMode.HALF_UP);
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

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
