package com.kfood.payment.app.gateway;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record CreatePixChargeGatewayCommand(
    UUID paymentId,
    UUID orderId,
    BigDecimal amount,
    String idempotencyKey,
    String correlationId,
    String description) {

  public CreatePixChargeGatewayCommand {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(orderId, "orderId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
  }
}
