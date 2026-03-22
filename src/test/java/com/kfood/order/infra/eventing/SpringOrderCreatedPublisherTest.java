package com.kfood.order.infra.eventing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kfood.order.app.OrderCreatedEvent;
import com.kfood.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SpringOrderCreatedPublisherTest {

  @Test
  void shouldPublishOrderCreatedEvent() {
    var applicationEventPublisher = mock(ApplicationEventPublisher.class);
    var publisher = new SpringOrderCreatedPublisher(applicationEventPublisher);
    var event =
        new OrderCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "PED-20260322-000001",
            OrderStatus.NEW,
            new BigDecimal("50.00"),
            OffsetDateTime.parse("2026-03-22T15:00:00Z"));

    publisher.publish(event);

    verify(applicationEventPublisher).publishEvent(event);
  }
}
