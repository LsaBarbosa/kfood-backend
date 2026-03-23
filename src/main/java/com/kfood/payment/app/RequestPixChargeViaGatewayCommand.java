package com.kfood.payment.app;

import java.math.BigDecimal;
import java.util.UUID;

public record RequestPixChargeViaGatewayCommand(
    String provider,
    UUID paymentId,
    UUID orderId,
    BigDecimal amount,
    String idempotencyKey,
    String correlationId,
    String description) {}
