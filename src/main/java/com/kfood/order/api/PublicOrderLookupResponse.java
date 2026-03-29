package com.kfood.order.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

public record PublicOrderLookupResponse(
    String orderNumber,
    OrderStatus status,
    PaymentStatusSnapshot paymentStatus,
    FulfillmentType fulfillmentType,
    BigDecimal subtotalAmount,
    BigDecimal deliveryFeeAmount,
    BigDecimal totalAmount,
    Instant createdAt,
    OffsetDateTime scheduledFor) {

  @JsonProperty("paymentStatusSnapshot")
  public PaymentStatusSnapshot paymentStatusSnapshot() {
    return paymentStatus;
  }
}
