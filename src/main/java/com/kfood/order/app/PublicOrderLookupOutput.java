package com.kfood.order.app;

import com.kfood.order.domain.FulfillmentType;
import com.kfood.order.domain.OrderStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

public record PublicOrderLookupOutput(
    String orderNumber,
    OrderStatus status,
    PaymentStatusSnapshot paymentStatusSnapshot,
    FulfillmentType fulfillmentType,
    BigDecimal subtotalAmount,
    BigDecimal deliveryFeeAmount,
    BigDecimal totalAmount,
    Instant createdAt,
    OffsetDateTime scheduledFor) {}
