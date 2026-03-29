package com.kfood.merchant.app;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryZoneOutput(
    UUID id, String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount, boolean active) {}
