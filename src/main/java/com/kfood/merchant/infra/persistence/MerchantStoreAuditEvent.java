package com.kfood.merchant.infra.persistence;

import com.kfood.merchant.domain.LegalDocumentType;
import com.kfood.merchant.domain.StoreStatus;
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
@Table(name = "merchant_store_audit_event")
public class MerchantStoreAuditEvent {

  @Id private UUID id;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  @Column(name = "actor_user_id", nullable = false)
  private UUID actorUserId;

  @Column(name = "event_type", nullable = false, length = 40)
  private String eventType;

  @Column(name = "entity_type", nullable = false, length = 40)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "before_status", length = 20)
  private StoreStatus beforeStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "after_status", length = 20)
  private StoreStatus afterStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", length = 40)
  private LegalDocumentType documentType;

  @Column(name = "document_version", length = 40)
  private String documentVersion;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  protected MerchantStoreAuditEvent() {}

  public MerchantStoreAuditEvent(
      UUID id,
      UUID storeId,
      UUID actorUserId,
      String eventType,
      String entityType,
      UUID entityId,
      Instant occurredAt,
      StoreStatus beforeStatus,
      StoreStatus afterStatus,
      LegalDocumentType documentType,
      String documentVersion,
      Instant acceptedAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.storeId = Objects.requireNonNull(storeId, "storeId is required");
    this.actorUserId = Objects.requireNonNull(actorUserId, "actorUserId is required");
    this.eventType = normalize(Objects.requireNonNull(eventType, "eventType is required"));
    this.entityType = normalize(Objects.requireNonNull(entityType, "entityType is required"));
    this.entityId = Objects.requireNonNull(entityId, "entityId is required");
    this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
    this.beforeStatus = beforeStatus;
    this.afterStatus = afterStatus;
    this.documentType = documentType;
    this.documentVersion = documentVersion == null ? null : normalize(documentVersion);
    this.acceptedAt = acceptedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getEntityType() {
    return entityType;
  }

  public UUID getEntityId() {
    return entityId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public StoreStatus getBeforeStatus() {
    return beforeStatus;
  }

  public StoreStatus getAfterStatus() {
    return afterStatus;
  }

  public LegalDocumentType getDocumentType() {
    return documentType;
  }

  public String getDocumentVersion() {
    return documentVersion;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  private String normalize(String value) {
    return value.trim();
  }
}
