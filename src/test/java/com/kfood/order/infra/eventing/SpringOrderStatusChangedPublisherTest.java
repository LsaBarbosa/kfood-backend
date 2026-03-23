package com.kfood.order.infra.eventing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.eventing.app.OrderStatusChangedFacts;
import com.kfood.eventing.app.OrderStatusChangedOutboxService;
import com.kfood.eventing.app.OutboxCreatedEvent;
import com.kfood.order.app.OrderStatusChangedEvent;
import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class SpringOrderStatusChangedPublisherTest {

  @Test
  void shouldStoreStatusChangeInOutboxAndPublishInternalNotification() {
    var orderStatusChangedOutboxService = mock(OrderStatusChangedOutboxService.class);
    var applicationEventPublisher = mock(ApplicationEventPublisher.class);
    var publisher =
        new SpringOrderStatusChangedPublisher(
            orderStatusChangedOutboxService, applicationEventPublisher);
    var event =
        new OrderStatusChangedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            OrderStatus.NEW,
            OrderStatus.PREPARING,
            Instant.parse("2026-03-23T13:45:00Z"));
    var outboxId = UUID.randomUUID();

    when(orderStatusChangedOutboxService.enqueue(
            org.mockito.ArgumentMatchers.any(OrderStatusChangedFacts.class)))
        .thenReturn(outboxId);

    publisher.publish(event);

    var factsCaptor = ArgumentCaptor.forClass(OrderStatusChangedFacts.class);
    verify(orderStatusChangedOutboxService).enqueue(factsCaptor.capture());
    verify(applicationEventPublisher).publishEvent(new OutboxCreatedEvent(outboxId));

    var facts = factsCaptor.getValue();
    assertThat(facts.orderId()).isEqualTo(event.orderId().toString());
    assertThat(facts.tenantId()).isEqualTo(event.storeId().toString());
    assertThat(facts.oldStatus()).isEqualTo("NEW");
    assertThat(facts.newStatus()).isEqualTo("PREPARING");
    assertThat(facts.changedAt()).isEqualTo(Instant.parse("2026-03-23T13:45:00Z"));
  }
}
