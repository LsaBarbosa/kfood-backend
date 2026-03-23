package com.kfood.eventing.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedOutboxService {

  private static final String AGGREGATE_TYPE = "ORDER";
  private static final String EVENT_TYPE = "order.created";
  private static final int EVENT_VERSION = 1;

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;
  private final CorrelationIdProvider correlationIdProvider;

  public OrderCreatedOutboxService(
      OutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper,
      CorrelationIdProvider correlationIdProvider) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
    this.correlationIdProvider = correlationIdProvider;
  }

  public UUID enqueue(OrderCreatedFacts facts) {
    var envelope =
        new EventEnvelope<>(
            UUID.randomUUID(),
            EVENT_TYPE,
            EVENT_VERSION,
            Instant.now().toString(),
            facts.tenantId(),
            correlationIdProvider.getOrCreate(),
            new OrderCreatedPayload(
                facts.orderId(), facts.orderNumber(), facts.status(), facts.totalAmount()));
    var outboxEvent =
        OutboxEvent.newPending(
            AGGREGATE_TYPE, facts.orderId(), EVENT_TYPE, EVENT_TYPE, serialize(envelope));

    outboxEventRepository.save(outboxEvent);
    return outboxEvent.getId();
  }

  private String serialize(EventEnvelope<OrderCreatedPayload> envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize order.created event", exception);
    }
  }
}
