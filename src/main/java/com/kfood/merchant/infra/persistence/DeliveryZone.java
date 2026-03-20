package com.kfood.merchant.infra.persistence;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "delivery_zone")
public class DeliveryZone extends AuditableEntity {

  @Id private UUID id;

  @NotNull @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @NotBlank @Size(max = 120) @Column(name = "zone_name", nullable = false, length = 120)
  private String zoneName;

  @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "fee_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal feeAmount;

  @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal minOrderAmount;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected DeliveryZone() {}

  public DeliveryZone(
      UUID id,
      Store store,
      String zoneName,
      BigDecimal feeAmount,
      BigDecimal minOrderAmount,
      boolean active) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.store = Objects.requireNonNull(store, "store is required");
    this.zoneName = normalize(Objects.requireNonNull(zoneName, "zoneName is required"));
    this.feeAmount = normalizeMoney(Objects.requireNonNull(feeAmount, "feeAmount is required"));
    this.minOrderAmount =
        normalizeMoney(Objects.requireNonNull(minOrderAmount, "minOrderAmount is required"));
    this.active = active;
    validateBusinessRules();
  }

  @PrePersist
  void prePersist() {
    validateBusinessRules();
  }

  public UUID getId() {
    return id;
  }

  public Store getStore() {
    return store;
  }

  public String getZoneName() {
    return zoneName;
  }

  public BigDecimal getFeeAmount() {
    return feeAmount;
  }

  public BigDecimal getMinOrderAmount() {
    return minOrderAmount;
  }

  public boolean isActive() {
    return active;
  }

  public void changeZoneName(String zoneName) {
    this.zoneName = normalize(Objects.requireNonNull(zoneName, "zoneName is required"));
    validateBusinessRules();
  }

  public void changeFeeAmount(BigDecimal feeAmount) {
    this.feeAmount = normalizeMoney(Objects.requireNonNull(feeAmount, "feeAmount is required"));
    validateBusinessRules();
  }

  public void changeMinOrderAmount(BigDecimal minOrderAmount) {
    this.minOrderAmount =
        normalizeMoney(Objects.requireNonNull(minOrderAmount, "minOrderAmount is required"));
    validateBusinessRules();
  }

  public void activate() {
    active = true;
  }

  public void deactivate() {
    active = false;
  }

  private void validateBusinessRules() {
    if (zoneName.isBlank()) {
      throw new IllegalArgumentException("zoneName must not be blank");
    }
    if (feeAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("feeAmount must be greater than or equal to zero");
    }
    if (minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("minOrderAmount must be greater than or equal to zero");
    }
  }

  private String normalize(String value) {
    return value.trim();
  }

  private BigDecimal normalizeMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
