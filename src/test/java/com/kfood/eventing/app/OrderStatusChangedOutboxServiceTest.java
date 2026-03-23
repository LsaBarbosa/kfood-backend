package com.kfood.eventing.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.eventing.domain.OutboxEventStatus;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderStatusChangedOutboxServiceTest {

  private OutboxEventRepository outboxEventRepository;
  private CorrelationIdProvider correlationIdProvider;
  private ObjectMapper objectMapper;
  private OrderStatusChangedOutboxService service;

  @BeforeEach
  void setUp() {
    outboxEventRepository = mock(OutboxEventRepository.class);
    correlationIdProvider = mock(CorrelationIdProvider.class);
    objectMapper = new ObjectMapper().findAndRegisterModules();

    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(correlationIdProvider.getOrCreate()).thenReturn("corr-status-1");

    service =
        new OrderStatusChangedOutboxService(
            outboxEventRepository, objectMapper, correlationIdProvider);
  }

  @Test
  void shouldCreatePendingOutboxEventWithStatusChangePayload() throws Exception {
    var changedAt = Instant.parse("2026-03-23T13:45:00Z");

    service.enqueue(
        new OrderStatusChangedFacts("ord-1", "store-10", "NEW", "PREPARING", changedAt));

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());

    var savedEvent = captor.getValue();
    assertThat(savedEvent.getPublicationStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(savedEvent.getEventType()).isEqualTo("order.status.changed");
    assertThat(savedEvent.getRoutingKey()).isEqualTo("order.status.changed");
    assertThat(savedEvent.getAggregateType()).isEqualTo("ORDER");
    assertThat(savedEvent.getAggregateId()).isEqualTo("ord-1");

    var root = objectMapper.readTree(savedEvent.getPayload());
    assertThat(root.get("eventType").asText()).isEqualTo("order.status.changed");
    assertThat(root.get("tenantId").asText()).isEqualTo("store-10");
    assertThat(root.get("correlationId").asText()).isEqualTo("corr-status-1");
    assertThat(root.get("occurredAt").asText()).isEqualTo("2026-03-23T13:45:00Z");

    var payload = root.get("payload");
    assertThat(payload.get("orderId").asText()).isEqualTo("ord-1");
    assertThat(payload.get("oldStatus").asText()).isEqualTo("NEW");
    assertThat(payload.get("newStatus").asText()).isEqualTo("PREPARING");
    assertThat(payload.get("changedAt").asText()).isEqualTo("2026-03-23T13:45:00Z");
  }
}
