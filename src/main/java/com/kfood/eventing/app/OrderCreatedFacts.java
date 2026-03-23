package com.kfood.eventing.app;

import java.math.BigDecimal;

public record OrderCreatedFacts(
    String orderId, String tenantId, String orderNumber, String status, BigDecimal totalAmount) {}
