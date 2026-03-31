package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

class PaymentWebhookEventPersistenceAdapterTest {

  private final PaymentWebhookEventRepository paymentWebhookEventRepository =
      mock(PaymentWebhookEventRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final PaymentWebhookEventPersistenceAdapter adapter =
      new PaymentWebhookEventPersistenceAdapter(paymentWebhookEventRepository, paymentRepository);

  @Test
  void shouldFindExistingWebhookEvent() {
    var existingEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    when(paymentWebhookEventRepository.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    var result = adapter.findByProviderNameAndExternalEventId("mock", "evt-123");

    assertThat(result).containsSame(existingEvent);
  }

  @Test
  void shouldFindWebhookEventById() {
    var existingEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    when(paymentWebhookEventRepository.findById(existingEvent.getId()))
        .thenReturn(Optional.of(existingEvent));

    var result = adapter.findById(existingEvent.getId());

    assertThat(result).containsSame(existingEvent);
  }

  @Test
  void shouldSaveReceivedWebhookEvent() {
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var eventId = UUID.randomUUID();
    var receivedAt = Instant.parse("2026-03-30T16:00:00Z");
    var result =
        adapter.saveReceivedEvent(
            eventId,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            receivedAt);

    ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(paymentWebhookEventRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(eventId);
    assertThat(captor.getValue().getProviderName()).isEqualTo("mock");
    assertThat(captor.getValue().getExternalEventId()).isEqualTo("evt-123");
    assertThat(captor.getValue().getEventType()).isEqualTo("PAYMENT_CONFIRMED");
    assertThat(captor.getValue().isSignatureValid()).isTrue();
    assertThat(captor.getValue().getRawPayload())
        .isEqualTo("{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}");
    assertThat(captor.getValue().getReceivedAt()).isEqualTo(receivedAt);
    assertThat(result).isSameAs(captor.getValue());
  }

  @Test
  void shouldTranslateJpaSystemExceptionWrappingUniqueViolation() {
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(
            new JpaSystemException(
                new GenericJDBCException(
                    "could not execute statement",
                    new SQLException(
                        "ERROR: duplicate key value violates unique constraint"
                            + " \"uk_payment_webhook_event_provider_external_event\"",
                        "23505"))));

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("Duplicate payment webhook event for provider and external event id")
        .hasCauseInstanceOf(JpaSystemException.class);
  }

  @Test
  void shouldTranslateWhenConstraintMessageAndSqlStateAppearInDifferentCauseLevels() {
    var exception =
        new JpaSystemException(
            new GenericJDBCException(
                "could not execute statement for constraint"
                    + " uk_payment_webhook_event_provider_external_event",
                new SQLException("duplicate key", "23505")));
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(exception);

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("Duplicate payment webhook event for provider and external event id")
        .hasCauseInstanceOf(JpaSystemException.class);
  }

  @Test
  void shouldKeepStableDataIntegrityViolationWhenRepositoryAlreadyTranslatesDuplicate() {
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(
            new DataIntegrityViolationException(
                "duplicate key value violates unique constraint"
                    + " \"uk_payment_webhook_event_provider_external_event\""));

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("Duplicate payment webhook event for provider and external event id")
        .hasCauseInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldNotTranslateDataIntegrityViolationExceptionWhenConstraintIsDifferent() {
    var exception =
        new DataIntegrityViolationException(
            "duplicate key value violates unique constraint \"uk_other_constraint\"");
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(exception);

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isSameAs(exception);
  }

  @Test
  void shouldNotTranslateJpaSystemExceptionWhenCauseIsNotUniqueViolation() {
    var exception =
        new JpaSystemException(
            new GenericJDBCException(
                "could not execute statement", new SQLException("generic failure", "08006")));
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(exception);

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isSameAs(exception);
  }

  @Test
  void shouldNotTranslateJpaSystemExceptionWhenUniqueViolationTargetsDifferentConstraint() {
    var exception =
        new JpaSystemException(
            new GenericJDBCException(
                "could not execute statement",
                new SQLException(
                    "ERROR: duplicate key value violates unique constraint"
                        + " \"uk_other_constraint\"",
                    "23505")));
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(exception);

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isSameAs(exception);
  }

  @Test
  void shouldNotTranslateWhenWebhookConstraintAppearsButSqlStateIsNotUniqueViolation() {
    var exception =
        new JpaSystemException(
            new GenericJDBCException(
                "could not execute statement for constraint"
                    + " uk_payment_webhook_event_provider_external_event",
                new SQLException("connection dropped while writing", "08006")));
    when(paymentWebhookEventRepository.saveAndFlush(any(PaymentWebhookEvent.class)))
        .thenThrow(exception);

    assertThatThrownBy(
            () ->
                adapter.saveReceivedEvent(
                    UUID.randomUUID(),
                    "mock",
                    "evt-123",
                    "PAYMENT_CONFIRMED",
                    true,
                    "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
                    Instant.parse("2026-03-30T16:00:00Z")))
        .isSameAs(exception);
  }

  @Test
  void shouldMarkWebhookEventAsProcessedAndAttachPayment() {
    var payment = mock(Payment.class);
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    when(paymentWebhookEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(paymentRepository.getReferenceById(
            UUID.fromString("11111111-1111-1111-1111-111111111111")))
        .thenReturn(payment);

    var result =
        adapter.markProcessed(
            event.getId(),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            Instant.parse("2026-03-30T16:00:00Z"));

    assertThat(result.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(((PaymentWebhookEvent) result).getPayment()).isSameAs(payment);
  }

  @Test
  void shouldMarkWebhookEventAsProcessedWithoutPayment() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_PENDING",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    when(paymentWebhookEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

    var result = adapter.markProcessed(event.getId(), null, Instant.parse("2026-03-30T16:00:00Z"));

    assertThat(result.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(((PaymentWebhookEvent) result).getPayment()).isNull();
    assertThat(result.getProcessedAt()).isEqualTo(Instant.parse("2026-03-30T16:00:00Z"));
  }

  @Test
  void shouldMarkWebhookEventAsFailedProcessing() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    when(paymentWebhookEventRepository.findById(event.getId())).thenReturn(Optional.of(event));

    var result = adapter.markFailedProcessing(event.getId(), Instant.parse("2026-03-30T16:00:00Z"));

    assertThat(result.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.PaymentWebhookProcessingStatus.FAILED_PROCESSING);
    assertThat(result.getProcessedAt()).isEqualTo(Instant.parse("2026-03-30T16:00:00Z"));
  }
}
