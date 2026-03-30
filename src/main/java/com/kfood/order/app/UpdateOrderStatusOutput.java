package com.kfood.order.app;

import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record UpdateOrderStatusOutput(
    UUID id,
    OrderStatus previousStatus,
    OrderStatus newStatus,
    Instant changedAt,
    UUID changedBy) {}
