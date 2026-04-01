package com.kfood.payment.api;

import com.kfood.payment.app.port.PaymentWebhookEventRecord;

public record PaymentWebhookResponse(
    boolean accepted, String processingStatus, String externalEventId) {

  static PaymentWebhookResponse from(PaymentWebhookEventRecord event) {
    return new PaymentWebhookResponse(
        true, event.getProcessingStatus().name(), event.getExternalEventId());
  }
}
