package com.kfood.order.infra.eventing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.eventing.app.OrderCreatedFacts;
import com.kfood.eventing.app.OrderCreatedOutboxService;
import com.kfood.eventing.app.OutboxCreatedEvent;
import com.kfood.order.app.OrderCreatedEvent;
import com.kfood.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class SpringOrderCreatedPublisherTest {

  @Test
  void shouldStoreOrderCreatedEventInOutboxAndPublishInternalNotification() {
    var orderCreatedOutboxService = mock(OrderCreatedOutboxService.class);
    var applicationEventPublisher = mock(ApplicationEventPublisher.class);
    var publisher =
        new SpringOrderCreatedPublisher(orderCreatedOutboxService, applicationEventPublisher);
    var event =
        new OrderCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "PED-20260322-000001",
            OrderStatus.NEW,
            new BigDecimal("50.00"),
            OffsetDateTime.parse("2026-03-22T15:00:00Z"));
    var outboxId = UUID.randomUUID();

    when(orderCreatedOutboxService.enqueue(
            org.mockito.ArgumentMatchers.any(OrderCreatedFacts.class)))
        .thenReturn(outboxId);

    publisher.publish(event);

    var factsCaptor = ArgumentCaptor.forClass(OrderCreatedFacts.class);
    verify(orderCreatedOutboxService).enqueue(factsCaptor.capture());
    verify(applicationEventPublisher).publishEvent(new OutboxCreatedEvent(outboxId));

    var facts = factsCaptor.getValue();
    assertThat(facts.orderId()).isEqualTo(event.orderId().toString());
    assertThat(facts.tenantId()).isEqualTo(event.storeId().toString());
    assertThat(facts.orderNumber()).isEqualTo(event.orderNumber());
    assertThat(facts.status()).isEqualTo(event.status().name());
    assertThat(facts.totalAmount()).isEqualByComparingTo("50.00");
  }
}
