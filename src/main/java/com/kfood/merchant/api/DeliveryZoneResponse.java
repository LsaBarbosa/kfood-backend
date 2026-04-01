package com.kfood.merchant.api;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryZoneResponse(
    UUID id, String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount, boolean active) {}
