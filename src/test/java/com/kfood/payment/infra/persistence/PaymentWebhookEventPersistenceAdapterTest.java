package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

class PaymentWebhookEventPersistenceAdapterTest {

  private final PaymentWebhookEventRepository paymentWebhookEventRepository =
      mock(PaymentWebhookEventRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
  private final PaymentWebhookEventPersistenceAdapter adapter =
      new PaymentWebhookEventPersistenceAdapter(
          paymentWebhookEventRepository, paymentRepository, jdbcTemplate);

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
  void shouldReturnInsertedWebhookEventWhenInsertReturnsId() {
    var insertedEventId = UUID.randomUUID();
    var insertedEvent =
        new PaymentWebhookEvent(
            insertedEventId,
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T16:00:00Z"));
    when(jdbcTemplate.query(
            anyString(),
            any(PreparedStatementSetter.class),
            org.mockito.ArgumentMatchers.<ResultSetExtractor<UUID>>any()))
        .thenReturn(insertedEventId);
    when(paymentWebhookEventRepository.findById(insertedEventId))
        .thenReturn(Optional.of(insertedEvent));

    var result =
        adapter.saveReceivedEvent(
            insertedEventId,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T16:00:00Z"));

    verify(paymentWebhookEventRepository).findById(insertedEventId);
    verify(paymentWebhookEventRepository, never())
        .findByProviderNameAndExternalEventId(anyString(), anyString());
    assertThat(result).isSameAs(insertedEvent);
  }

  @Test
  void shouldReturnExistingWebhookEventWhenInsertReturnsNullByConflict() {
    var existingEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T16:00:00Z"));
    when(jdbcTemplate.query(
            anyString(),
            any(PreparedStatementSetter.class),
            org.mockito.ArgumentMatchers.<ResultSetExtractor<UUID>>any()))
        .thenReturn(null);
    when(paymentWebhookEventRepository.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    var result =
        adapter.saveReceivedEvent(
            UUID.randomUUID(),
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.parse("2026-03-30T16:00:00Z"));

    verify(paymentWebhookEventRepository).findByProviderNameAndExternalEventId("mock", "evt-123");
    verify(paymentWebhookEventRepository, never()).findById(any(UUID.class));
    assertThat(result).isSameAs(existingEvent);
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
  void shouldMarkWebhookEventAsIgnored() {
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

    var result = adapter.markIgnored(event.getId(), Instant.parse("2026-03-30T16:00:00Z"));

    assertThat(result.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.PaymentWebhookProcessingStatus.IGNORED);
    assertThat(result.getProcessedAt()).isEqualTo(Instant.parse("2026-03-30T16:00:00Z"));
  }

  @Test
  void shouldMarkWebhookEventAsFailed() {
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

    var result = adapter.markFailed(event.getId(), Instant.parse("2026-03-30T16:00:00Z"));

    assertThat(result.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.PaymentWebhookProcessingStatus.FAILED);
    assertThat(result.getProcessedAt()).isEqualTo(Instant.parse("2026-03-30T16:00:00Z"));
  }
}
