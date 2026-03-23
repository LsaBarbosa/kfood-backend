package com.kfood.eventing.app;

import java.math.BigDecimal;

public record OrderCreatedPayload(
    String orderId, String orderNumber, String status, BigDecimal totalAmount) {}
