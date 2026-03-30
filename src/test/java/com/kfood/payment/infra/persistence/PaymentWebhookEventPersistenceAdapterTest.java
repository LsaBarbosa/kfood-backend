package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentWebhookEventPersistenceAdapterTest {

  private final PaymentWebhookEventRepository paymentWebhookEventRepository =
      mock(PaymentWebhookEventRepository.class);
  private final PaymentWebhookEventPersistenceAdapter adapter =
      new PaymentWebhookEventPersistenceAdapter(paymentWebhookEventRepository);

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
  void shouldSaveReceivedWebhookEvent() {
    when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var eventId = UUID.randomUUID();
    var receivedAt = Instant.parse("2026-03-30T16:00:00Z");
    var result =
        adapter.saveReceivedEvent(
            eventId,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            receivedAt);

    ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
    verify(paymentWebhookEventRepository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(eventId);
    assertThat(captor.getValue().getProviderName()).isEqualTo("mock");
    assertThat(captor.getValue().getExternalEventId()).isEqualTo("evt-123");
    assertThat(captor.getValue().getEventType()).isEqualTo("PAYMENT_CONFIRMED");
    assertThat(captor.getValue().getRawPayload())
        .isEqualTo("{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}");
    assertThat(captor.getValue().getReceivedAt()).isEqualTo(receivedAt);
    assertThat(result).isSameAs(captor.getValue());
  }
}
