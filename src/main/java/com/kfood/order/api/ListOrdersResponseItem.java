package com.kfood.order.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListOrdersResponseItem(
    UUID id,
    String orderNumber,
    OrderStatus status,
    PaymentStatusSnapshot paymentStatus,
    String customerName,
    BigDecimal totalAmount,
    Instant createdAt) {

  @JsonProperty("paymentStatusSnapshot")
  public PaymentStatusSnapshot paymentStatusSnapshot() {
    return paymentStatus;
  }
}
