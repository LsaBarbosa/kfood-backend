package com.kfood.merchant.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateDeliveryZoneRequest(
    @NotBlank @Size(max = 120) String zoneName,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal feeAmount,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal minOrderAmount,
    @NotNull Boolean active) {}
