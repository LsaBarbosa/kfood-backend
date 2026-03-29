package com.kfood.payment.app;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderPixPaymentCommand(
    UUID orderId, BigDecimal amount, String provider, String idempotencyKey) {}
