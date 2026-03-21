package com.kfood.checkout.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuoteCheckoutResponse(
    String quoteId,
    UUID storeId,
    BigDecimal subtotalAmount,
    BigDecimal deliveryFeeAmount,
    BigDecimal totalAmount,
    int estimatedPreparationMinutes,
    List<String> messages) {}
