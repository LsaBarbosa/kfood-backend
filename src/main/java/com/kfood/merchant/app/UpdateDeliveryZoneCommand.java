package com.kfood.merchant.app;

import java.math.BigDecimal;

public record UpdateDeliveryZoneCommand(
    String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount, boolean active) {}
