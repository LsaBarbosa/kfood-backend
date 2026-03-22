package com.kfood.payment.api;

import com.kfood.payment.domain.WebhookProcessingStatus;

public record PaymentWebhookResponse(
    boolean accepted, WebhookProcessingStatus processingStatus, String externalEventId) {}
