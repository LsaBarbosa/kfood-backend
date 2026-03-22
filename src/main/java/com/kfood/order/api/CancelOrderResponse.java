package com.kfood.order.api;

import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record CancelOrderResponse(UUID id, OrderStatus status, Instant canceledAt, String reason) {}
