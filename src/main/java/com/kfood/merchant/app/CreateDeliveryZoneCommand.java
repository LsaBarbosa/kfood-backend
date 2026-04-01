package com.kfood.merchant.app;

import java.math.BigDecimal;

public record CreateDeliveryZoneCommand(
    String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount, boolean active) {}
