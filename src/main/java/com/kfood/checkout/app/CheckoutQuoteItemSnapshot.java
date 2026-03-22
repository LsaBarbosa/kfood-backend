package com.kfood.checkout.app;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutQuoteItemSnapshot(
    UUID productId,
    String productNameSnapshot,
    BigDecimal unitPriceSnapshot,
    Integer quantity,
    String notes,
    List<CheckoutQuoteOptionSnapshot> options) {}
