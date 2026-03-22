package com.kfood.payment.infra.persistence;

import com.kfood.payment.domain.WebhookProcessingStatus;
import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
          columnNames = {"provider_name", "external_event_id"}),
      @UniqueConstraint(
          name = "uk_payment_webhook_event_provider_idempotency_key",
          columnNames = {"provider_name", "idempotency_key"})
    },
    indexes = {
      @Index(name = "idx_payment_webhook_event_payment_id", columnList = "payment_id"),
      @Index(
          name = "idx_payment_webhook_event_provider_status_received_at",
          columnList = "provider_name, processing_status, received_at")
    })
public class PaymentWebhookEvent extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id")
  private Payment payment;

  @Column(name = "provider_name", nullable = false, length = 100)
  private String providerName;

  @Column(name = "external_event_id", length = 150)
  private String externalEventId;

  @Column(name = "idempotency_key", nullable = false, length = 200)
  private String idempotencyKey;

  @Column(name = "signature_valid")
  private Boolean signatureValid;

  @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
  private String rawPayload;

  @Enumerated(EnumType.STRING)
  @Column(name = "processing_status", nullable = false, length = 30)
  private WebhookProcessingStatus processingStatus;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  protected PaymentWebhookEvent() {}

  private PaymentWebhookEvent(
      UUID id,
      Payment payment,
      String providerName,
      String externalEventId,
      String idempotencyKey,
      Boolean signatureValid,
      String rawPayload,
      WebhookProcessingStatus processingStatus,
      Instant receivedAt,
      Instant processedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.payment = payment;
    this.providerName = requireText(providerName, "providerName");
    this.externalEventId = normalizeNullable(externalEventId);
    this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    this.signatureValid = signatureValid;
    this.rawPayload = requireText(rawPayload, "rawPayload");
    this.processingStatus =
        Objects.requireNonNull(processingStatus, "processingStatus must not be null");
    this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    this.processedAt = processedAt;
  }

  public static PaymentWebhookEvent received(
      Payment payment,
      String providerName,
      String externalEventId,
      String idempotencyKey,
      String rawPayload) {
    return new PaymentWebhookEvent(
        UUID.randomUUID(),
        payment,
        providerName,
        externalEventId,
        idempotencyKey,
        null,
        rawPayload,
        WebhookProcessingStatus.RECEIVED,
        Instant.now(),
        null);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    providerName = requireText(providerName, "providerName");
    externalEventId = normalizeNullable(externalEventId);
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    rawPayload = requireText(rawPayload, "rawPayload");
    processingStatus =
        Objects.requireNonNull(processingStatus, "processingStatus must not be null");
    receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
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

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public Boolean getSignatureValid() {
    return signatureValid;
  }

  public String getRawPayload() {
    return rawPayload;
  }

  public WebhookProcessingStatus getProcessingStatus() {
    return processingStatus;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }

  public void defineSignatureValidation(boolean signatureValid) {
    this.signatureValid = signatureValid;
  }

  public void markProcessed() {
    processingStatus = WebhookProcessingStatus.PROCESSED;
    processedAt = Instant.now();
  }

  public void markIgnored() {
    processingStatus = WebhookProcessingStatus.IGNORED;
    processedAt = Instant.now();
  }

  public void markFailed() {
    processingStatus = WebhookProcessingStatus.FAILED;
    processedAt = Instant.now();
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value.trim();
  }

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
