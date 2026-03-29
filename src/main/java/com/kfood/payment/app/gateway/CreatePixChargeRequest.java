package com.kfood.payment.app.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePixChargeRequest(
    UUID paymentId,
    UUID orderId,
    BigDecimal amount,
    String idempotencyKey,
    String correlationId,
    String description) {}
