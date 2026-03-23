package com.kfood.order.infra.eventing;

import com.kfood.eventing.app.OrderStatusChangedFacts;
import com.kfood.eventing.app.OrderStatusChangedOutboxService;
import com.kfood.eventing.app.OutboxCreatedEvent;
import com.kfood.order.app.OrderStatusChangedEvent;
import com.kfood.order.app.OrderStatusChangedPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderStatusChangedPublisher implements OrderStatusChangedPublisher {

  private final OrderStatusChangedOutboxService orderStatusChangedOutboxService;
  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringOrderStatusChangedPublisher(
      OrderStatusChangedOutboxService orderStatusChangedOutboxService,
      ApplicationEventPublisher applicationEventPublisher) {
    this.orderStatusChangedOutboxService = orderStatusChangedOutboxService;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(OrderStatusChangedEvent event) {
    var outboxEventId =
        orderStatusChangedOutboxService.enqueue(
            new OrderStatusChangedFacts(
                event.orderId().toString(),
                event.storeId().toString(),
                event.previousStatus().name(),
                event.newStatus().name(),
                event.changedAt()));
    applicationEventPublisher.publishEvent(new OutboxCreatedEvent(outboxEventId));
  }
}
