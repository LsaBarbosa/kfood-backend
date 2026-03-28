package com.kfood.payment.infra.persistence;

import com.kfood.order.infra.persistence.SalesOrder;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusTransitionException;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

  @Column(name = "provider_name", length = 80)
  private String providerName;

  @Column(name = "provider_reference", length = 120)
  private String providerReference;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "qr_code_payload", length = 4000)
  private String qrCodePayload;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  protected Payment() {}

  public static Payment createPending(
      UUID id, SalesOrder order, PaymentMethod paymentMethod, BigDecimal amount) {
    return new Payment(
        id, order, paymentMethod, null, null, PaymentStatus.PENDING, amount, null, null, null);
  }

  public static Payment createPendingPix(UUID id, SalesOrder order, BigDecimal amount) {
    return createPending(id, order, PaymentMethod.PIX, amount);
  }

  public Payment(
      UUID id,
      SalesOrder order,
      PaymentMethod paymentMethod,
      String providerName,
      String providerReference,
      PaymentStatus status,
      BigDecimal amount,
      String qrCodePayload,
      Instant confirmedAt,
      Instant expiresAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.order = Objects.requireNonNull(order, "order is required");
    this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod is required");
    this.providerName = normalizeNullable(providerName);
    this.providerReference = normalizeNullable(providerReference);
    this.status = Objects.requireNonNull(status, "status is required");
    this.amount = normalizeMoney(amount);
    this.qrCodePayload = normalizeNullable(qrCodePayload);
    this.confirmedAt = confirmedAt;
    this.expiresAt = expiresAt;
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

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void attachPixChargeData(
      String providerName,
      String providerReference,
      String qrCodePayload,
      OffsetDateTime expiresAt) {
    this.providerName = normalizeNullable(providerName);
    this.providerReference = normalizeNullable(providerReference);
    this.qrCodePayload = normalizeNullable(qrCodePayload);
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required").toInstant();
  }

  public boolean canTransitionTo(PaymentStatus targetStatus) {
    var validatedTargetStatus =
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
    if (status == validatedTargetStatus) {
      return true;
    }

    return switch (status) {
      case PENDING ->
          validatedTargetStatus == PaymentStatus.CONFIRMED
              || validatedTargetStatus == PaymentStatus.FAILED
              || validatedTargetStatus == PaymentStatus.CANCELED
              || validatedTargetStatus == PaymentStatus.EXPIRED;
      case CONFIRMED, FAILED, CANCELED, EXPIRED -> false;
    };
  }

  public void changeStatus(PaymentStatus targetStatus) {
    changeStatus(targetStatus, Instant.now());
  }

  public void changeStatus(PaymentStatus targetStatus, Instant currentTimestamp) {
    if (!canTransitionTo(targetStatus)) {
      throw new PaymentStatusTransitionException(status, targetStatus);
    }
    status = targetStatus;
    if (targetStatus == PaymentStatus.CONFIRMED && confirmedAt == null) {
      confirmedAt = Objects.requireNonNull(currentTimestamp, "currentTimestamp must not be null");
    }
  }

  private static BigDecimal normalizeMoney(BigDecimal value) {
    return Objects.requireNonNull(value, "amount is required").setScale(2, RoundingMode.HALF_UP);
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
