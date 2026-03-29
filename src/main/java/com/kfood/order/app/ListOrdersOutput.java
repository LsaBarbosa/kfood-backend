package com.kfood.order.app;

import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListOrdersOutput(
    List<Item> items, int page, int size, long totalElements, int totalPages, List<String> sort) {

  public record Item(
      UUID id,
      String orderNumber,
      OrderStatus status,
      PaymentStatusSnapshot paymentStatusSnapshot,
      String customerName,
      BigDecimal totalAmount,
      Instant createdAt) {}
}
