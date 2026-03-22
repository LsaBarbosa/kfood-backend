package com.kfood.order.api;

import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record UpdateOrderStatusResponse(
    UUID id,
    OrderStatus previousStatus,
    OrderStatus newStatus,
    Instant changedAt,
    UUID changedBy) {}
