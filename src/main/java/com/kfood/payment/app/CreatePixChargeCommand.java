package com.kfood.payment.app;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePixChargeCommand(
    String providerCode,
    UUID paymentId,
    UUID orderId,
    BigDecimal amount,
    String idempotencyKey,
    String correlationId,
    String description) {}
