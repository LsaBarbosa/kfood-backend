package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookEventPersistenceAdapter implements PaymentWebhookEventPersistencePort {

  private static final String INSERT_RECEIVED_EVENT_SQL =
      """
      INSERT INTO payment_webhook_event (
          id,
          payment_id,
          provider_name,
          external_event_id,
          event_type,
          signature_valid,
          raw_payload,
          processing_status,
          received_at,
          processed_at,
          created_at,
          updated_at
      ) VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?)
      ON CONFLICT (provider_name, external_event_id) DO NOTHING
      RETURNING id
      """;

  private final PaymentWebhookEventRepository paymentWebhookEventRepository;
  private final PaymentRepository paymentRepository;
  private final JdbcTemplate jdbcTemplate;

  public PaymentWebhookEventPersistenceAdapter(
      PaymentWebhookEventRepository paymentWebhookEventRepository,
      PaymentRepository paymentRepository,
      JdbcTemplate jdbcTemplate) {
    this.paymentWebhookEventRepository = paymentWebhookEventRepository;
    this.paymentRepository = paymentRepository;
    this.jdbcTemplate = jdbcTemplate;
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
    var event =
        new PaymentWebhookEvent(
            eventId,
            null,
            providerName,
            externalEventId,
            eventType,
            signatureValid,
            rawPayload,
            receivedAt);
    var auditTimestamp = Instant.now();

    UUID insertedEventId =
        jdbcTemplate.query(
            INSERT_RECEIVED_EVENT_SQL,
            preparedStatement -> {
              preparedStatement.setObject(1, event.getId());
              preparedStatement.setString(2, event.getProviderName());
              preparedStatement.setString(3, event.getExternalEventId());
              preparedStatement.setString(4, event.getEventType());
              preparedStatement.setBoolean(5, event.isSignatureValid());
              preparedStatement.setString(6, event.getRawPayload());
              preparedStatement.setString(7, PaymentWebhookProcessingStatus.RECEIVED.name());
              preparedStatement.setTimestamp(8, Timestamp.from(event.getReceivedAt()));
              preparedStatement.setTimestamp(9, Timestamp.from(auditTimestamp));
              preparedStatement.setTimestamp(10, Timestamp.from(auditTimestamp));
            },
            resultSet -> resultSet.next() ? resultSet.getObject("id", UUID.class) : null);

    if (insertedEventId != null) {
      return paymentWebhookEventRepository.findById(insertedEventId).orElseThrow();
    }

    return paymentWebhookEventRepository
        .findByProviderNameAndExternalEventId(event.getProviderName(), event.getExternalEventId())
        .orElseThrow();
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
}
