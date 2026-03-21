package com.kfood.merchant.api;

import java.math.BigDecimal;

public record PublicDeliveryZoneResponse(
    String zoneName, BigDecimal feeAmount, BigDecimal minOrderAmount) {}
