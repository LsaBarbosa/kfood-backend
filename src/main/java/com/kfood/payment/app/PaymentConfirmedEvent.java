package com.kfood.payment.app;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentConfirmedEvent(
    UUID paymentId,
    UUID orderId,
    UUID storeId,
    String providerName,
    BigDecimal amount,
    OffsetDateTime occurredAt) {}
