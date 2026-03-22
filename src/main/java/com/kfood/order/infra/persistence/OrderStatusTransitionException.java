package com.kfood.order.infra.persistence;

import com.kfood.order.domain.OrderStatus;

public class OrderStatusTransitionException extends RuntimeException {

  public OrderStatusTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
    super("Invalid order status transition from " + currentStatus + " to " + targetStatus);
  }
}
