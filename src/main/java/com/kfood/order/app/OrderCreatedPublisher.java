package com.kfood.order.app;

public interface OrderCreatedPublisher {

  void publish(OrderCreatedEvent event);
}
