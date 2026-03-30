package com.kfood.order.domain;

public class OrderStatusTransitionException extends RuntimeException {

  public OrderStatusTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
    super("Invalid order status transition from " + currentStatus + " to " + targetStatus);
  }
}
