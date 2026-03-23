package com.kfood.eventing.infra.persistence;

import com.kfood.eventing.domain.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

  @Id private UUID id;

  @Column(name = "aggregate_type", nullable = false, length = 80)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 80)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 120)
  private String eventType;

  @Column(name = "routing_key", nullable = false, length = 120)
  private String routingKey;

  @Column(name = "payload", nullable = false, columnDefinition = "text")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "publication_status", nullable = false, length = 30)
  private OutboxEventStatus publicationStatus;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  protected OutboxEvent() {}

  private OutboxEvent(
      UUID id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String routingKey,
      String payload,
      OutboxEventStatus publicationStatus,
      int attempts,
      String lastError,
      Instant createdAt,
      Instant publishedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.aggregateType = requireText(aggregateType, "aggregateType");
    this.aggregateId = requireText(aggregateId, "aggregateId");
    this.eventType = requireText(eventType, "eventType");
    this.routingKey = requireText(routingKey, "routingKey");
    this.payload = requireText(payload, "payload");
    this.publicationStatus =
        Objects.requireNonNull(publicationStatus, "publicationStatus must not be null");
    this.attempts = attempts;
    this.lastError = truncate(normalizeNullable(lastError), 500);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.publishedAt = publishedAt;
  }

  public static OutboxEvent newPending(
      String aggregateType,
      String aggregateId,
      String eventType,
      String routingKey,
      String payload) {
    return new OutboxEvent(
        UUID.randomUUID(),
        aggregateType,
        aggregateId,
        eventType,
        routingKey,
        payload,
        OutboxEventStatus.PENDING,
        0,
        null,
        Instant.now(),
        null);
  }

  @PrePersist
  @PreUpdate
  void validateLifecycle() {
    aggregateType = requireText(aggregateType, "aggregateType");
    aggregateId = requireText(aggregateId, "aggregateId");
    eventType = requireText(eventType, "eventType");
    routingKey = requireText(routingKey, "routingKey");
    payload = requireText(payload, "payload");
    publicationStatus =
        Objects.requireNonNull(publicationStatus, "publicationStatus must not be null");
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    lastError = truncate(normalizeNullable(lastError), 500);
  }

  public UUID getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public String getPayload() {
    return payload;
  }

  public OutboxEventStatus getPublicationStatus() {
    return publicationStatus;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void markPublished(Instant publishedAt) {
    publicationStatus = OutboxEventStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    lastError = null;
  }

  public void registerFailure(String errorMessage) {
    attempts++;
    lastError = truncate(normalizeNullable(errorMessage), 500);
    publicationStatus = OutboxEventStatus.PENDING;
    publishedAt = null;
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

  private static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }

    return value.substring(0, maxLength);
  }
}
