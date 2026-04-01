package com.kfood.payment.app.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookEventPersistencePort {

  Optional<PaymentWebhookEventRecord> findByProviderNameAndExternalEventId(
      String providerName, String externalEventId);

  Optional<PaymentWebhookEventRecord> findById(UUID eventId);

  PaymentWebhookEventRecord saveReceivedEvent(
      UUID eventId,
      String providerName,
      String externalEventId,
      String eventType,
      boolean signatureValid,
      String rawPayload,
      Instant receivedAt);

  PaymentWebhookEventRecord markProcessed(UUID eventId, UUID paymentId, Instant processedAt);

  PaymentWebhookEventRecord markFailedProcessing(UUID eventId, Instant processedAt);
}
