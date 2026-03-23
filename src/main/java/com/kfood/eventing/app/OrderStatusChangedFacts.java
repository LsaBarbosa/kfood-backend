package com.kfood.eventing.app;

import java.time.Instant;

public record OrderStatusChangedFacts(
    String orderId, String tenantId, String oldStatus, String newStatus, Instant changedAt) {}
