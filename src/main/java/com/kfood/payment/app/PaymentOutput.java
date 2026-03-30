package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentOutput(
    UUID id,
    UUID orderId,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    BigDecimal amount,
    Instant createdAt) {}
