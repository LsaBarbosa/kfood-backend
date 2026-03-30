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
  private final PaymentRepository paymentRepository;

  public PaymentWebhookEventPersistenceAdapter(
      PaymentWebhookEventRepository paymentWebhookEventRepository,
      PaymentRepository paymentRepository) {
    this.paymentWebhookEventRepository = paymentWebhookEventRepository;
    this.paymentRepository = paymentRepository;
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

  @Override
  public PaymentWebhookEventRecord markProcessed(
      UUID eventId, UUID paymentId, Instant processedAt) {
    var event = paymentWebhookEventRepository.findById(eventId).orElseThrow();
    event.attachPayment(paymentRepository.getReferenceById(paymentId));
    event.markProcessed(processedAt);
    return event;
  }

  @Override
  public PaymentWebhookEventRecord markFailedProcessing(UUID eventId, Instant processedAt) {
    var event = paymentWebhookEventRepository.findById(eventId).orElseThrow();
    event.markFailedProcessing(processedAt);
    return event;
  }
}
