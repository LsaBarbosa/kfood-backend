package com.kfood.checkout.app;

import com.kfood.order.domain.FulfillmentType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CheckoutQuoteSnapshot(
    UUID quoteId,
    UUID storeId,
    UUID customerId,
    FulfillmentType fulfillmentType,
    UUID addressId,
    BigDecimal subtotalAmount,
    BigDecimal deliveryFeeAmount,
    BigDecimal totalAmount,
    List<CheckoutQuoteItemSnapshot> items,
    OffsetDateTime expiresAt) {}
