package com.kfood.checkout.infra.persistence;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "checkout_quote")
public class CheckoutQuote extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "fulfillment_type", nullable = false, length = 20)
  private FulfillmentType fulfillmentType;

  @Column(name = "address_id")
  private UUID addressId;

  @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotalAmount;

  @Column(name = "delivery_fee_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal deliveryFeeAmount;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @OneToMany(
      mappedBy = "quote",
      cascade = jakarta.persistence.CascadeType.ALL,
      orphanRemoval = true)
  private final List<CheckoutQuoteItem> items = new ArrayList<>();

  protected CheckoutQuote() {}

  public CheckoutQuote(
      UUID id,
      UUID storeId,
      UUID customerId,
      FulfillmentType fulfillmentType,
      UUID addressId,
      BigDecimal subtotalAmount,
      BigDecimal deliveryFeeAmount,
      BigDecimal totalAmount,
      OffsetDateTime expiresAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.storeId = Objects.requireNonNull(storeId, "storeId is required");
    this.customerId = Objects.requireNonNull(customerId, "customerId is required");
    this.fulfillmentType = Objects.requireNonNull(fulfillmentType, "fulfillmentType is required");
    this.addressId = addressId;
    this.subtotalAmount = normalizeMoney(subtotalAmount, "subtotalAmount");
    this.deliveryFeeAmount = normalizeMoney(deliveryFeeAmount, "deliveryFeeAmount");
    this.totalAmount = normalizeMoney(totalAmount, "totalAmount");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
  }

  @PrePersist
  void validateLifecycle() {
    if (totalAmount.compareTo(subtotalAmount.add(deliveryFeeAmount)) != 0) {
      throw new IllegalArgumentException(
          "totalAmount must be equal to subtotalAmount + deliveryFeeAmount");
    }
  }

  public void addItem(CheckoutQuoteItem item) {
    var validatedItem = Objects.requireNonNull(item, "item is required");
    validatedItem.attachToQuote(this);
    items.add(validatedItem);
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public FulfillmentType getFulfillmentType() {
    return fulfillmentType;
  }

  public UUID getAddressId() {
    return addressId;
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

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public List<CheckoutQuoteItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  private BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
    return Objects.requireNonNull(value, fieldName + " is required")
        .setScale(2, java.math.RoundingMode.HALF_UP);
  }
}
