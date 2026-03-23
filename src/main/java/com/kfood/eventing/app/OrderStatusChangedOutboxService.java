package com.kfood.eventing.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusChangedOutboxService {

  private static final String AGGREGATE_TYPE = "ORDER";
  private static final String EVENT_TYPE = "order.status.changed";
  private static final int EVENT_VERSION = 1;

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;
  private final CorrelationIdProvider correlationIdProvider;

  public OrderStatusChangedOutboxService(
      OutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper,
      CorrelationIdProvider correlationIdProvider) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
    this.correlationIdProvider = correlationIdProvider;
  }

  public UUID enqueue(OrderStatusChangedFacts facts) {
    var occurredAt = facts.changedAt().toString();
    var envelope =
        new EventEnvelope<>(
            UUID.randomUUID(),
            EVENT_TYPE,
            EVENT_VERSION,
            occurredAt,
            facts.tenantId(),
            correlationIdProvider.getOrCreate(),
            new OrderStatusChangedPayload(
                facts.orderId(), facts.oldStatus(), facts.newStatus(), occurredAt));
    var outboxEvent =
        OutboxEvent.newPending(
            AGGREGATE_TYPE, facts.orderId(), EVENT_TYPE, EVENT_TYPE, serialize(envelope));

    outboxEventRepository.save(outboxEvent);
    return outboxEvent.getId();
  }

  private String serialize(EventEnvelope<OrderStatusChangedPayload> envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize order.status.changed event", exception);
    }
  }
}
