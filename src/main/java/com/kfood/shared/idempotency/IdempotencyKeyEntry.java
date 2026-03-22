package com.kfood.shared.idempotency;

import com.kfood.shared.infra.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "idempotency_key")
public class IdempotencyKeyEntry extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "store_id", nullable = false)
  private UUID storeId;

  @Column(name = "scope", nullable = false, length = 100)
  private String scope;

  @Column(name = "key_value", nullable = false, length = 128)
  private String keyValue;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "response_body", columnDefinition = "text")
  private String responseBody;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  protected IdempotencyKeyEntry() {}

  public IdempotencyKeyEntry(
      UUID id,
      UUID storeId,
      String scope,
      String keyValue,
      String requestHash,
      OffsetDateTime expiresAt) {
    this.id = Objects.requireNonNull(id, "id is required");
    this.storeId = Objects.requireNonNull(storeId, "storeId is required");
    this.scope = Objects.requireNonNull(scope, "scope is required").trim();
    this.keyValue = Objects.requireNonNull(keyValue, "keyValue is required").trim();
    this.requestHash = Objects.requireNonNull(requestHash, "requestHash is required");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
  }

  public UUID getStoreId() {
    return storeId;
  }

  public String getScope() {
    return scope;
  }

  public String getKeyValue() {
    return keyValue;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void complete(String responseBody, int httpStatus) {
    this.responseBody = Objects.requireNonNull(responseBody, "responseBody is required");
    this.httpStatus = httpStatus;
  }
}
