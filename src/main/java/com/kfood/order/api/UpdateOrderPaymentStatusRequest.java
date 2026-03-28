package com.kfood.order.api;

import com.kfood.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderPaymentStatusRequest(@NotNull PaymentStatus newStatus) {}
