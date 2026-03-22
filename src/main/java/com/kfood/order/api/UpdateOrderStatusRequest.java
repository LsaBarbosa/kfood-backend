package com.kfood.order.api;

import com.kfood.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
    @NotNull OrderStatus newStatus, @Size(max = 255) String reason) {}
