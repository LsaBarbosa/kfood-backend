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
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderCreatedOutboxServiceTest {

  private OutboxEventRepository outboxEventRepository;
  private CorrelationIdProvider correlationIdProvider;
  private ObjectMapper objectMapper;
  private OrderCreatedOutboxService service;

  @BeforeEach
  void setUp() {
    outboxEventRepository = mock(OutboxEventRepository.class);
    correlationIdProvider = mock(CorrelationIdProvider.class);
    objectMapper = new ObjectMapper().findAndRegisterModules();

    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(correlationIdProvider.getOrCreate()).thenReturn("corr-123");

    service =
        new OrderCreatedOutboxService(outboxEventRepository, objectMapper, correlationIdProvider);
  }

  @Test
  void shouldCreatePendingOutboxEventWithMinimumPayload() throws Exception {
    var outboxId =
        service.enqueue(
            new OrderCreatedFacts("101", "10", "ORD-2026-0001", "NEW", new BigDecimal("57.90")));

    var captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());

    var savedEvent = captor.getValue();
    assertThat(outboxId).isEqualTo(savedEvent.getId());
    assertThat(savedEvent.getPublicationStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(savedEvent.getEventType()).isEqualTo("order.created");
    assertThat(savedEvent.getRoutingKey()).isEqualTo("order.created");
    assertThat(savedEvent.getAggregateType()).isEqualTo("ORDER");
    assertThat(savedEvent.getAggregateId()).isEqualTo("101");

    var root = objectMapper.readTree(savedEvent.getPayload());
    assertThat(root.get("eventType").asText()).isEqualTo("order.created");
    assertThat(root.get("version").asInt()).isEqualTo(1);
    assertThat(root.get("tenantId").asText()).isEqualTo("10");
    assertThat(root.get("correlationId").asText()).isEqualTo("corr-123");

    var payload = root.get("payload");
    assertThat(payload.get("orderId").asText()).isEqualTo("101");
    assertThat(payload.get("orderNumber").asText()).isEqualTo("ORD-2026-0001");
    assertThat(payload.get("status").asText()).isEqualTo("NEW");
    assertThat(payload.get("totalAmount").decimalValue()).isEqualByComparingTo("57.90");
  }
}
