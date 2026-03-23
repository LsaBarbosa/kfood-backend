package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaymentWebhookEventTest {

  @Test
  void shouldCreateWebhookEventWithReceivedInitialStatus() {
    var event =
        PaymentWebhookEvent.received(
            null,
            " MOCK_PSP ",
            " evt_payment_confirmed_001 ",
            " evt_payment_confirmed_001 ",
            "{\"type\":\"payment.confirmed\"}");

    assertThat(event.getId()).isNotNull();
    assertThat(event.getPayment()).isNull();
    assertThat(event.getProviderName()).isEqualTo("MOCK_PSP");
    assertThat(event.getExternalEventId()).isEqualTo("evt_payment_confirmed_001");
    assertThat(event.getIdempotencyKey()).isEqualTo("evt_payment_confirmed_001");
    assertThat(event.getRawPayload()).isEqualTo("{\"type\":\"payment.confirmed\"}");
    assertThat(event.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.WebhookProcessingStatus.RECEIVED);
    assertThat(event.getReceivedAt()).isNotNull();
    assertThat(event.getProcessedAt()).isNull();
    assertThat(event.getSignatureValid()).isNull();
  }

  @Test
  void shouldUpdateWebhookProcessingLifecycle() {
    var processed =
        PaymentWebhookEvent.received(null, "MOCK_PSP", "evt-1", "evt-1", "{\"type\":\"ok\"}");
    processed.markProcessed();
    assertThat(processed.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.WebhookProcessingStatus.PROCESSED);
    assertThat(processed.getProcessedAt()).isNotNull();

    var ignored =
        PaymentWebhookEvent.received(null, "MOCK_PSP", "evt-2", "evt-2", "{\"type\":\"ok\"}");
    ignored.markIgnored();
    assertThat(ignored.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.WebhookProcessingStatus.IGNORED);
    assertThat(ignored.getProcessedAt()).isNotNull();

    var failed =
        PaymentWebhookEvent.received(null, "MOCK_PSP", "evt-3", "evt-3", "{\"type\":\"ok\"}");
    failed.markFailed();
    assertThat(failed.getProcessingStatus())
        .isEqualTo(com.kfood.payment.domain.WebhookProcessingStatus.FAILED);
    assertThat(failed.getProcessedAt()).isNotNull();
  }

  @Test
  void shouldAllowDefiningSignatureValidation() {
    var event =
        PaymentWebhookEvent.received(null, "MOCK_PSP", "evt-1", "evt-1", "{\"type\":\"ok\"}");

    event.defineSignatureValidation(true);

    assertThat(event.getSignatureValid()).isTrue();
  }

  @Test
  void shouldAttachWebhookToPayment() {
    var event =
        PaymentWebhookEvent.received(null, "MOCK_PSP", "evt-1", "evt-1", "{\"type\":\"ok\"}");
    var payment = Mockito.mock(Payment.class);

    event.attachToPayment(payment);

    assertThat(event.getPayment()).isEqualTo(payment);
  }

  @Test
  void shouldRejectInvalidWebhookEventArguments() {
    assertThatThrownBy(() -> PaymentWebhookEvent.received(null, null, "evt", "evt", "{}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerName must not be blank");
    assertThatThrownBy(() -> PaymentWebhookEvent.received(null, "MOCK_PSP", "evt", " ", "{}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("idempotencyKey must not be blank");
    assertThatThrownBy(() -> PaymentWebhookEvent.received(null, "MOCK_PSP", "evt", "evt", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rawPayload must not be blank");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<PaymentWebhookEvent> constructor =
        PaymentWebhookEvent.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    assertThat(constructor.newInstance()).isNotNull();
  }

  @Test
  void shouldNormalizeBlankExternalEventIdToNull() {
    var event = PaymentWebhookEvent.received(null, "MOCK_PSP", " ", "evt", "{\"type\":\"ok\"}");

    assertThat(event.getExternalEventId()).isNull();
  }
}
