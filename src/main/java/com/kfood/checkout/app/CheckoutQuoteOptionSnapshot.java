package com.kfood.checkout.app;

import java.math.BigDecimal;

public record CheckoutQuoteOptionSnapshot(
    String optionNameSnapshot, BigDecimal extraPriceSnapshot, Integer quantity) {}
