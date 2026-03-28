package com.kfood.order.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.util.UUID;

public record UpdateOrderPaymentStatusResponse(
    UUID paymentId,
    UUID orderId,
    PaymentStatus paymentStatus,
    PaymentStatusSnapshot paymentStatusSnapshot) {

  @JsonProperty("technicalPaymentStatus")
  public PaymentStatus technicalPaymentStatus() {
    return paymentStatus;
  }
}
