package com.kfood.order.app;

import com.kfood.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record CancelOrderOutput(UUID id, OrderStatus status, Instant canceledAt, String reason) {}
