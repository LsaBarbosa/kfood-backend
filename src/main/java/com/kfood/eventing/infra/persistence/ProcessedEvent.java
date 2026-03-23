package com.kfood.eventing.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

  @Id private UUID id;

  @Column(name = "consumer_name", nullable = false, length = 120)
  private String consumerName;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "event_type", nullable = false, length = 120)
  private String eventType;

  @Column(name = "aggregate_id", length = 80)
  private String aggregateId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  protected ProcessedEvent() {}

  private ProcessedEvent(
      UUID id,
      String consumerName,
      UUID eventId,
      String eventType,
      String aggregateId,
      Instant processedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.consumerName = requireText(consumerName, "consumerName");
    this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
    this.eventType = requireText(eventType, "eventType");
    this.aggregateId = normalizeNullable(aggregateId);
    this.processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
  }

  public static ProcessedEvent create(
      String consumerName, UUID eventId, String eventType, String aggregateId) {
    return new ProcessedEvent(
        UUID.randomUUID(), consumerName, eventId, eventType, aggregateId, Instant.now());
  }

  @PrePersist
  void validateLifecycle() {
    consumerName = requireText(consumerName, "consumerName");
    eventType = requireText(eventType, "eventType");
    aggregateId = normalizeNullable(aggregateId);
    processedAt = Objects.requireNonNull(processedAt, "processedAt must not be null");
    eventId = Objects.requireNonNull(eventId, "eventId must not be null");
  }

  public UUID getId() {
    return id;
  }

  public String getConsumerName() {
    return consumerName;
  }

  public UUID getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public Instant getProcessedAt() {
    return processedAt;
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
