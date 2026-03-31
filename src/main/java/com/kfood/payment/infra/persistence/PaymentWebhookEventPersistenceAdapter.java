package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookEventPersistenceAdapter implements PaymentWebhookEventPersistencePort {

  private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
  private static final String WEBHOOK_EVENT_UNIQUE_CONSTRAINT =
      "uk_payment_webhook_event_provider_external_event";

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
  public Optional<PaymentWebhookEventRecord> findById(UUID eventId) {
    return paymentWebhookEventRepository
        .findById(eventId)
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
    try {
      return paymentWebhookEventRepository.saveAndFlush(
          new PaymentWebhookEvent(
              eventId,
              null,
              providerName,
              externalEventId,
              eventType,
              signatureValid,
              rawPayload,
              receivedAt));
    } catch (RuntimeException exception) {
      if (isDuplicateWebhookEvent(exception)) {
        throw new DataIntegrityViolationException(
            "Duplicate payment webhook event for provider and external event id", exception);
      }
      throw exception;
    }
  }

  @Override
  public PaymentWebhookEventRecord markProcessed(
      UUID eventId, UUID paymentId, Instant processedAt) {
    var event = paymentWebhookEventRepository.findById(eventId).orElseThrow();
    if (paymentId != null) {
      event.attachPayment(paymentRepository.getReferenceById(paymentId));
    }
    event.markProcessed(processedAt);
    return event;
  }

  @Override
  public PaymentWebhookEventRecord markFailedProcessing(UUID eventId, Instant processedAt) {
    var event = paymentWebhookEventRepository.findById(eventId).orElseThrow();
    event.markFailedProcessing(processedAt);
    return event;
  }

  private boolean isDuplicateWebhookEvent(Throwable exception) {
    for (Throwable current = exception; current != null; current = current.getCause()) {
      if (current instanceof DataIntegrityViolationException
          && hasWebhookConstraintMessage(current)) {
        return true;
      }
      if (current instanceof SQLException sqlException
          && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())
          && hasWebhookConstraintMessage(sqlException)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasWebhookConstraintMessage(Throwable throwable) {
    return throwable.getMessage() != null
        && throwable.getMessage().contains(WEBHOOK_EVENT_UNIQUE_CONSTRAINT);
  }
}
