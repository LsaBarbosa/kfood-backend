package com.kfood.payment.app;

import java.util.UUID;

public record PaymentWebhookRegisteredEvent(
    UUID eventId, String providerReference, String eventType) {}
