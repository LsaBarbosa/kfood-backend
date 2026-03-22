package com.kfood.order.app;

import com.kfood.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    UUID storeId,
    String orderNumber,
    OrderStatus status,
    BigDecimal totalAmount,
    OffsetDateTime occurredAt) {}
