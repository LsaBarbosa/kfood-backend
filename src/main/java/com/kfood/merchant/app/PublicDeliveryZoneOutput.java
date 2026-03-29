package com.kfood.merchant.app;

import java.math.BigDecimal;

public record PublicDeliveryZoneOutput(
    String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount) {}
