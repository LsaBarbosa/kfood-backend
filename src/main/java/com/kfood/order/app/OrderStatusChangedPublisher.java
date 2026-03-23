package com.kfood.order.app;

public interface OrderStatusChangedPublisher {

  void publish(OrderStatusChangedEvent event);
}
