package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookEventPersistenceAdapter implements PaymentWebhookEventPersistencePort {

  private final PaymentWebhookEventRepository paymentWebhookEventRepository;

  public PaymentWebhookEventPersistenceAdapter(
      PaymentWebhookEventRepository paymentWebhookEventRepository) {
    this.paymentWebhookEventRepository = paymentWebhookEventRepository;
  }

  @Override
  public Optional<PaymentWebhookEventRecord> findByProviderNameAndExternalEventId(
      String providerName, String externalEventId) {
    return paymentWebhookEventRepository
        .findByProviderNameAndExternalEventId(providerName, externalEventId)
        .map(PaymentWebhookEventRecord.class::cast);
  }

  @Override
  public PaymentWebhookEventRecord saveReceivedEvent(
      UUID eventId,
      String providerName,
      String externalEventId,
      String eventType,
      boolean signatureValid,
      String rawPayload,
      Instant receivedAt) {
    return paymentWebhookEventRepository.save(
        new PaymentWebhookEvent(
            eventId,
            null,
            providerName,
            externalEventId,
            eventType,
            signatureValid,
            rawPayload,
            receivedAt));
  }
}
