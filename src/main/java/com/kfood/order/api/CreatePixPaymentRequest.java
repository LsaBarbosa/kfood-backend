package com.kfood.order.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePixPaymentRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount, @NotBlank String provider) {}
