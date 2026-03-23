package com.kfood.eventing.app;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentConfirmedFacts(
    String paymentId,
    String orderId,
    String tenantId,
    String providerName,
    BigDecimal amount,
    OffsetDateTime occurredAt) {}
