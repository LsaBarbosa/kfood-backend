package com.kfood.payment.infra.persistence;

import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
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
@Table(name = "payment")
public class Payment extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private SalesOrder order;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  @Column(name = "provider_name", length = 100)
  private String providerName;

  @Column(name = "provider_reference", length = 255)
  private String providerReference;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "qr_code_payload", columnDefinition = "text")
  private String qrCodePayload;

  @Column(name = "confirmed_at")
  private OffsetDateTime confirmedAt;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  protected Payment() {}

  private Payment(
      UUID id,
      SalesOrder order,
      PaymentMethod paymentMethod,
      String providerName,
      String providerReference,
      String qrCodePayload) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");
    this.providerName = normalizeNullable(providerName);
    this.providerReference = normalizeNullable(providerReference);
    this.qrCodePayload = normalizeNullable(qrCodePayload);
    amount = normalizeMoney(order.getTotalAmount(), "amount");
    status = PaymentStatus.PENDING;
  }

  public static Payment create(
      UUID id,
      SalesOrder order,
      PaymentMethod paymentMethod,
      String providerName,
      String providerReference,
      String qrCodePayload) {
    return new Payment(id, order, paymentMethod, providerName, providerReference, qrCodePayload);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  public UUID getId() {
    return id;
  }

  public SalesOrder getOrder() {
    return order;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public String getProviderName() {
    return providerName;
  }

  public String getProviderReference() {
    return providerReference;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getQrCodePayload() {
    return qrCodePayload;
  }

  public OffsetDateTime getConfirmedAt() {
    return confirmedAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void attachPixCharge(
      String providerName,
      String providerReference,
      String qrCodePayload,
      OffsetDateTime expiresAt) {
    this.providerName = requireText(providerName, "providerName");
    this.providerReference = requireText(providerReference, "providerReference");
    this.qrCodePayload = requireText(qrCodePayload, "qrCodePayload");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  public void markConfirmed(OffsetDateTime confirmedAt) {
    status = PaymentStatus.CONFIRMED;
    this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
  }

  public void markFailed() {
    status = PaymentStatus.FAILED;
    confirmedAt = null;
  }

  public void markCanceled() {
    status = PaymentStatus.CANCELED;
    confirmedAt = null;
  }

  public void markExpired() {
    status = PaymentStatus.EXPIRED;
    confirmedAt = null;
  }

  private void validateBusinessRules() {
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("amount must not be negative");
    }
  }

  private static BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
    var normalized = Objects.requireNonNull(value, fieldName + " must not be null");
    return normalized.setScale(2, RoundingMode.HALF_UP);
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String requireText(String value, String fieldName) {
    var normalized = normalizeNullable(value);

    if (normalized == null) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return normalized;
  }
}
