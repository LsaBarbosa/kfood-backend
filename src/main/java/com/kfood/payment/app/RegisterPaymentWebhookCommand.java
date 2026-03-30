package com.kfood.payment.app;

public record RegisterPaymentWebhookCommand(String provider, String rawPayload) {}
