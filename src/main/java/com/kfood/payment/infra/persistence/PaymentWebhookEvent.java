package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "payment_webhook_event",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_payment_webhook_event_provider_external_event",
          columnNames = {"provider_name", "external_event_id"})
    })
public class PaymentWebhookEvent extends AuditableEntity implements PaymentWebhookEventRecord {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id")
  private Payment payment;

  @Column(name = "provider_name", nullable = false, length = 80)
  private String providerName;

  @Column(name = "external_event_id", nullable = false, length = 120)
  private String externalEventId;

  @Column(name = "event_type", length = 120)
  private String eventType;

  @Column(name = "signature_valid", nullable = false)
  private boolean signatureValid;

  @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
  private String rawPayload;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 20)
  private PaymentWebhookProcessingStatus processingStatus;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  protected PaymentWebhookEvent() {}

  public PaymentWebhookEvent(
      UUID id,
      Payment payment,
      String providerName,
      String externalEventId,
      String eventType,
      boolean signatureValid,
      String rawPayload,
      Instant receivedAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.payment = payment;
    this.providerName = normalizeRequired(providerName, "providerName");
    this.externalEventId = normalizeRequired(externalEventId, "externalEventId");
    this.eventType = normalizeNullable(eventType);
    this.signatureValid = signatureValid;
    this.rawPayload = normalizeRequired(rawPayload, "rawPayload");
    this.processingStatus = PaymentWebhookProcessingStatus.RECEIVED;
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt is required");
    this.processedAt = null;
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    validateBusinessRules();
  }

  public UUID getId() {
    return id;
  }

  public Payment getPayment() {
    return payment;
  }

  public String getProviderName() {
    return providerName;
  }

  public String getExternalEventId() {
    return externalEventId;
  }

  public String getEventType() {
    return eventType;
  }

  public boolean isSignatureValid() {
    return signatureValid;
  }

  public String getRawPayload() {
    return rawPayload;
  }

  public PaymentWebhookProcessingStatus getProcessingStatus() {
    return processingStatus;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void attachPayment(Payment payment) {
    this.payment = Objects.requireNonNull(payment, "payment is required");
  }

  public void markProcessed(Instant processedAt) {
    this.processingStatus = PaymentWebhookProcessingStatus.PROCESSED;
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt is required");
    validateBusinessRules();
  }

  public void markIgnored(Instant processedAt) {
    this.processingStatus = PaymentWebhookProcessingStatus.IGNORED;
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt is required");
    validateBusinessRules();
  }

  public void markFailed(Instant processedAt) {
    this.processingStatus = PaymentWebhookProcessingStatus.FAILED;
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt is required");
    validateBusinessRules();
  }

  private void validateBusinessRules() {
    if (providerName.isBlank()) {
      throw new IllegalArgumentException("providerName must not be blank");
    }
    if (externalEventId.isBlank()) {
      throw new IllegalArgumentException("externalEventId must not be blank");
    }
    if (rawPayload.isBlank()) {
      throw new IllegalArgumentException("rawPayload must not be blank");
    }
    if (processingStatus == null) {
      throw new IllegalArgumentException("processingStatus is required");
    }
    if (receivedAt == null) {
      throw new IllegalArgumentException("receivedAt is required");
    }
    if ((processingStatus == PaymentWebhookProcessingStatus.PROCESSED
            || processingStatus == PaymentWebhookProcessingStatus.IGNORED
            || processingStatus == PaymentWebhookProcessingStatus.FAILED)
        && processedAt == null) {
      throw new IllegalArgumentException("processedAt is required when event is finalized");
    }
    if (processingStatus == PaymentWebhookProcessingStatus.RECEIVED && processedAt != null) {
      throw new IllegalArgumentException("processedAt must be null when event is received");
    }
  }

  private static String normalizeRequired(String value, String fieldName) {
    var normalized = Objects.requireNonNull(value, fieldName + " is required").trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
