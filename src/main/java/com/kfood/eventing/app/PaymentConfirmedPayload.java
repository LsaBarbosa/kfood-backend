package com.kfood.eventing.app;

import java.math.BigDecimal;

public record PaymentConfirmedPayload(
    String paymentId, String orderId, String providerName, BigDecimal amount) {}
