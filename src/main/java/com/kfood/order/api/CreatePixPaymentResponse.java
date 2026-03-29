package com.kfood.order.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreatePixPaymentResponse(
    UUID paymentId,
    UUID orderId,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    String providerReference,
    String qrCodePayload,
    OffsetDateTime expiresAt) {

  @JsonProperty("paymentMethodSnapshot")
  public PaymentMethod paymentMethodSnapshot() {
    return paymentMethod;
  }

  @JsonProperty("technicalPaymentStatus")
  public PaymentStatus technicalPaymentStatus() {
    return status;
  }

  @JsonProperty("paymentStatusSnapshot")
  public PaymentStatusSnapshot paymentStatusSnapshot() {
    return mapToSnapshot(status);
  }

  private static PaymentStatusSnapshot mapToSnapshot(PaymentStatus paymentStatus) {
    return switch (paymentStatus) {
      case PENDING -> PaymentStatusSnapshot.PENDING;
      case CONFIRMED -> PaymentStatusSnapshot.PAID;
      case FAILED, CANCELED, EXPIRED -> PaymentStatusSnapshot.FAILED;
    };
  }
}
