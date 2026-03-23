package com.kfood.eventing.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.eventing.domain.OutboxEventStatus;
import com.kfood.eventing.infra.config.EventingOutboxProperties;
import com.kfood.eventing.infra.messaging.RabbitOutboxPublisher;
import com.kfood.eventing.infra.persistence.OutboxEvent;
import com.kfood.eventing.infra.persistence.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class OutboxRelayServiceTest {

  private OutboxEventRepository outboxEventRepository;
  private RabbitOutboxPublisher rabbitOutboxPublisher;
  private OutboxRelayService service;

  @BeforeEach
  void setUp() {
    outboxEventRepository = mock(OutboxEventRepository.class);
    rabbitOutboxPublisher = mock(RabbitOutboxPublisher.class);

    when(outboxEventRepository.save(any(OutboxEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service =
        new OutboxRelayService(
            outboxEventRepository, rabbitOutboxPublisher, new EventingOutboxProperties(50, 5000));
  }

  @Test
  void shouldPublishPendingOrderCreatedEvent() {
    var event =
        OutboxEvent.newPending(
            "ORDER", "101", "order.created", "order.created", "{\"eventType\":\"order.created\"}");

    when(outboxEventRepository.findByPublicationStatusOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(event)));

    service.publishPendingBatch();

    verify(rabbitOutboxPublisher).publish(event);
    verify(outboxEventRepository).save(event);
    assertThat(event.getPublicationStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    assertThat(event.getPublishedAt()).isBeforeOrEqualTo(Instant.now());
    assertThat(event.getLastError()).isNull();
  }

  @Test
  void shouldKeepEventPendingWhenPublishFails() {
    var event =
        OutboxEvent.newPending(
            "ORDER", "101", "order.created", "order.created", "{\"eventType\":\"order.created\"}");

    when(outboxEventRepository.findByPublicationStatusOrderByCreatedAtAsc(
            eq(OutboxEventStatus.PENDING), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(event)));
    doThrow(new IllegalStateException("RabbitMQ unavailable"))
        .when(rabbitOutboxPublisher)
        .publish(event);

    service.publishPendingBatch();

    verify(rabbitOutboxPublisher).publish(event);
    verify(outboxEventRepository).save(event);
    assertThat(event.getPublicationStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(event.getAttempts()).isEqualTo(1);
    assertThat(event.getLastError()).contains("RabbitMQ unavailable");
    assertThat(event.getPublishedAt()).isNull();
  }

  @Test
  void shouldPublishPendingEventById() {
    var outboxId = UUID.randomUUID();
    var event =
        OutboxEvent.newPending(
            "ORDER", "101", "order.created", "order.created", "{\"eventType\":\"order.created\"}");

    when(outboxEventRepository.findById(outboxId)).thenReturn(Optional.of(event));

    service.publishById(outboxId);

    verify(rabbitOutboxPublisher).publish(event);
    verify(outboxEventRepository).save(event);
    assertThat(event.getPublicationStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
  }
}
