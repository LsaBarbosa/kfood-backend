package com.kfood.eventing.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PaymentConfirmedOutboxService {

  private static final String AGGREGATE_TYPE = "PAYMENT";
  private static final String EVENT_TYPE = "payment.confirmed";
  private static final int EVENT_VERSION = 1;

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;
  private final CorrelationIdProvider correlationIdProvider;

  public PaymentConfirmedOutboxService(
      OutboxEventRepository outboxEventRepository,
      ObjectMapper objectMapper,
      CorrelationIdProvider correlationIdProvider) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
    this.correlationIdProvider = correlationIdProvider;
  }

  public UUID enqueueOnce(PaymentConfirmedFacts facts) {
    var dedupKey = dedupKey(facts.paymentId());

    return outboxEventRepository
        .findByDedupKey(dedupKey)
        .map(OutboxEvent::getId)
        .orElseGet(() -> createNew(facts, dedupKey));
  }

  private UUID createNew(PaymentConfirmedFacts facts, String dedupKey) {
    var envelope =
        new EventEnvelope<>(
            UUID.randomUUID(),
            EVENT_TYPE,
            EVENT_VERSION,
            facts.occurredAt().toString(),
            facts.tenantId(),
            correlationIdProvider.getOrCreate(),
            new PaymentConfirmedPayload(
                facts.paymentId(), facts.orderId(), facts.providerName(), facts.amount()));
    var outboxEvent =
        OutboxEvent.newPending(
            AGGREGATE_TYPE,
            facts.paymentId(),
            EVENT_TYPE,
            EVENT_TYPE,
            serialize(envelope),
            dedupKey);

    try {
      outboxEventRepository.save(outboxEvent);
      return outboxEvent.getId();
    } catch (DataIntegrityViolationException exception) {
      return outboxEventRepository
          .findByDedupKey(dedupKey)
          .map(OutboxEvent::getId)
          .orElseThrow(() -> exception);
    }
  }

  private String serialize(EventEnvelope<PaymentConfirmedPayload> envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize payment.confirmed event", exception);
    }
  }

  private String dedupKey(String paymentId) {
    return EVENT_TYPE + ":" + paymentId;
  }
}
