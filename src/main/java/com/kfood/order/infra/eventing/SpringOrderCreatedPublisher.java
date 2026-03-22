package com.kfood.order.infra.eventing;

import com.kfood.order.app.OrderCreatedEvent;
import com.kfood.order.app.OrderCreatedPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderCreatedPublisher implements OrderCreatedPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringOrderCreatedPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(OrderCreatedEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}
