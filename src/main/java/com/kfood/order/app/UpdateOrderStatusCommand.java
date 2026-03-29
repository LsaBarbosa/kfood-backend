package com.kfood.order.app;

import com.kfood.order.domain.OrderStatus;

public record UpdateOrderStatusCommand(OrderStatus newStatus, String reason) {}
