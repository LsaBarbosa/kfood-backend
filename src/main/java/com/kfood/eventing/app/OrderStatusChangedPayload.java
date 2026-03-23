package com.kfood.eventing.app;

public record OrderStatusChangedPayload(
    String orderId, String oldStatus, String newStatus, String changedAt) {}
