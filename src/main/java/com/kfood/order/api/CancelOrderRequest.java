package com.kfood.order.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
    @NotBlank(message = "must not be blank") @Size(max = 255, message = "must not be greater than 255 characters") String reason) {}
