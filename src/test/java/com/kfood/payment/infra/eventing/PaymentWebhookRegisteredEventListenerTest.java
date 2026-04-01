package com.kfood.payment.infra.eventing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.app.PaymentWebhookRegisteredEvent;
import com.kfood.payment.app.ProcessConfirmedPaymentWebhookUseCase;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentWebhookRegisteredEventListenerTest {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort =
      mock(PaymentWebhookEventPersistencePort.class);
  private final ProcessConfirmedPaymentWebhookUseCase processConfirmedPaymentWebhookUseCase =
      mock(ProcessConfirmedPaymentWebhookUseCase.class);
  private final PaymentWebhookRegisteredEventListener listener =
      new PaymentWebhookRegisteredEventListener(
          paymentWebhookEventPersistencePort, processConfirmedPaymentWebhookUseCase);

  @Test
  void shouldProcessRegisteredWebhookAfterCommitWhenEventIsStillReceived() {
    var event =
        new PaymentWebhookRegisteredEvent(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "charge-123",
            "PAYMENT_CONFIRMED");
    var persistedEvent = receivedWebhookEvent(event.eventId());
    when(paymentWebhookEventPersistencePort.findById(event.eventId()))
        .thenReturn(Optional.of(persistedEvent));

    listener.on(event);

    verify(processConfirmedPaymentWebhookUseCase).execute(persistedEvent, "charge-123");
  }

  @Test
  void shouldSkipProcessingWhenRegisteredWebhookCannotBeLoaded() {
    var event =
        new PaymentWebhookRegisteredEvent(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "charge-123",
            "PAYMENT_CONFIRMED");
    when(paymentWebhookEventPersistencePort.findById(event.eventId())).thenReturn(Optional.empty());

    listener.on(event);

    verify(processConfirmedPaymentWebhookUseCase, never()).execute(any(), any());
  }

  @Test
  void shouldSkipProcessingWhenWebhookWasAlreadyProcessed() {
    var event =
        new PaymentWebhookRegisteredEvent(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "charge-123",
            "PAYMENT_CONFIRMED");
    var persistedEvent = receivedWebhookEvent(event.eventId());
    persistedEvent.markProcessed(Instant.parse("2026-03-30T16:05:00Z"));
    when(paymentWebhookEventPersistencePort.findById(event.eventId()))
        .thenReturn(Optional.of(persistedEvent));

    listener.on(event);

    verify(processConfirmedPaymentWebhookUseCase, never()).execute(persistedEvent, "charge-123");
  }

  private PaymentWebhookEvent receivedWebhookEvent(UUID eventId) {
    return new PaymentWebhookEvent(
        eventId,
        null,
        "mock",
        "evt-123",
        "PAYMENT_CONFIRMED",
        true,
        "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
        Instant.parse("2026-03-30T16:00:00Z"));
  }
}
