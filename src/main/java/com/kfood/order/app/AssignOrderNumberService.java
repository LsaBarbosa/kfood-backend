package com.kfood.order.app;

import com.kfood.order.app.port.OrderNumberTarget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignOrderNumberService {

  private final OrderNumberGenerator orderNumberGenerator;

  public AssignOrderNumberService(OrderNumberGenerator orderNumberGenerator) {
    this.orderNumberGenerator = orderNumberGenerator;
  }

  @Transactional
  public void assignIfMissing(OrderNumberTarget order) {
    if (order.getOrderNumber() != null && !order.getOrderNumber().isBlank()) {
      return;
    }

    order.assignOrderNumber(orderNumberGenerator.next(order));
  }
}
