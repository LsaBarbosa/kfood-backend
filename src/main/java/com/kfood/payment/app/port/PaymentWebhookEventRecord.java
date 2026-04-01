package com.kfood.payment.app.port;

import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import java.time.Instant;
import java.util.UUID;

public interface PaymentWebhookEventRecord {

  UUID getId();

  String getProviderName();

  String getExternalEventId();

  String getEventType();

  boolean isSignatureValid();

  String getRawPayload();

  PaymentWebhookProcessingStatus getProcessingStatus();

  Instant getReceivedAt();

  Instant getProcessedAt();
}
