package com.kfood.order.infra.eventing;

import com.kfood.eventing.app.OrderCreatedFacts;
import com.kfood.eventing.app.OrderCreatedOutboxService;
import com.kfood.eventing.app.OutboxCreatedEvent;
import com.kfood.order.app.OrderCreatedEvent;
import com.kfood.order.app.OrderCreatedPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderCreatedPublisher implements OrderCreatedPublisher {

  private final OrderCreatedOutboxService orderCreatedOutboxService;
  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringOrderCreatedPublisher(
      OrderCreatedOutboxService orderCreatedOutboxService,
      ApplicationEventPublisher applicationEventPublisher) {
    this.orderCreatedOutboxService = orderCreatedOutboxService;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(OrderCreatedEvent event) {
    var outboxEventId =
        orderCreatedOutboxService.enqueue(
            new OrderCreatedFacts(
                event.orderId().toString(),
                event.storeId().toString(),
                event.orderNumber(),
                event.status().name(),
                event.totalAmount()));
    applicationEventPublisher.publishEvent(new OutboxCreatedEvent(outboxEventId));
  }
}
