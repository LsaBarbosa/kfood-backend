package com.kfood.order.api;

import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePublicOrderResponse(
    UUID id,
    String orderNumber,
    OrderStatus status,
    PaymentStatusSnapshot paymentStatusSnapshot,
    BigDecimal subtotalAmount,
    BigDecimal deliveryFeeAmount,
    BigDecimal totalAmount,
    Instant createdAt) {}
