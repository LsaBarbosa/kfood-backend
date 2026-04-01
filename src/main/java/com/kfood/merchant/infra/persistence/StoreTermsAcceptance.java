package com.kfood.merchant.infra.persistence;

import com.kfood.merchant.domain.LegalDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "store_terms_acceptance")
public class StoreTermsAcceptance {

  @Id private UUID id;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  @Column(name = "accepted_by_user_id", nullable = false)
  private UUID acceptedByUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 40)
  private LegalDocumentType documentType;

  @Column(name = "document_version", nullable = false, length = 40)
  private String documentVersion;

  @Column(name = "accepted_at", nullable = false)
  private Instant acceptedAt;

  @Column(name = "request_ip", nullable = false, length = 45)
  private String requestIp;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected StoreTermsAcceptance() {}

  public StoreTermsAcceptance(
      UUID id,
      UUID storeId,
      UUID acceptedByUserId,
      LegalDocumentType documentType,
      String documentVersion,
      Instant acceptedAt,
      String requestIp) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.storeId = Objects.requireNonNull(storeId, "storeId is required");
    this.acceptedByUserId =
        Objects.requireNonNull(acceptedByUserId, "acceptedByUserId is required");
    this.documentType = Objects.requireNonNull(documentType, "documentType is required");
    this.documentVersion =
        normalize(Objects.requireNonNull(documentVersion, "documentVersion is required"));
    this.acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt is required");
    this.requestIp = normalize(Objects.requireNonNull(requestIp, "requestIp is required"));
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UUID getAcceptedByUserId() {
    return acceptedByUserId;
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

  public String getRequestIp() {
    return requestIp;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  private String normalize(String value) {
    return value.trim();
  }
}
