package com.kfood.payment.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentWebhookEventTest {

  @Test
  void shouldCreateValidWebhookEvent() {
    var receivedAt = Instant.parse("2026-03-30T10:15:00Z");
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "  mock-psp  ",
            "  evt-123  ",
            "  PAYMENT_CONFIRMED  ",
            true,
            "  {\"id\":\"evt-123\"}  ",
            receivedAt);

    assertThat(event.getId()).isNotNull();
    assertThat(event.getPayment()).isNull();
    assertThat(event.getProviderName()).isEqualTo("mock-psp");
    assertThat(event.getExternalEventId()).isEqualTo("evt-123");
    assertThat(event.getEventType()).isEqualTo("PAYMENT_CONFIRMED");
    assertThat(event.isSignatureValid()).isTrue();
    assertThat(event.getRawPayload()).isEqualTo("{\"id\":\"evt-123\"}");
    assertThat(event.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.RECEIVED);
    assertThat(event.getReceivedAt()).isEqualTo(receivedAt);
    assertThat(event.getProcessedAt()).isNull();
    assertThat(event.getCreatedAt()).isNull();
    assertThat(event.getUpdatedAt()).isNull();
  }

  @Test
  void shouldNormalizeOptionalEventTypeToNullWhenBlank() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "provider",
            "evt-123",
            "   ",
            false,
            "{\"ok\":true}",
            Instant.parse("2026-03-30T10:15:00Z"));

    assertThat(event.getEventType()).isNull();
  }

  @Test
  void shouldRejectInvalidRequiredFields() {
    assertThatThrownBy(
            () ->
                new PaymentWebhookEvent(
                    null,
                    null,
                    "provider",
                    "evt-123",
                    null,
                    true,
                    "{\"ok\":true}",
                    Instant.parse("2026-03-30T10:15:00Z")))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("id is required");

    assertThatThrownBy(
            () ->
                new PaymentWebhookEvent(
                    UUID.randomUUID(),
                    null,
                    "   ",
                    "evt-123",
                    null,
                    true,
                    "{\"ok\":true}",
                    Instant.parse("2026-03-30T10:15:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerName must not be blank");

    assertThatThrownBy(
            () ->
                new PaymentWebhookEvent(
                    UUID.randomUUID(),
                    null,
                    "provider",
                    "   ",
                    null,
                    true,
                    "{\"ok\":true}",
                    Instant.parse("2026-03-30T10:15:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("externalEventId must not be blank");

    assertThatThrownBy(
            () ->
                new PaymentWebhookEvent(
                    UUID.randomUUID(),
                    null,
                    "provider",
                    "evt-123",
                    null,
                    true,
                    "   ",
                    Instant.parse("2026-03-30T10:15:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("rawPayload must not be blank");

    assertThatThrownBy(
            () ->
                new PaymentWebhookEvent(
                    UUID.randomUUID(),
                    null,
                    "provider",
                    "evt-123",
                    null,
                    true,
                    "{\"ok\":true}",
                    null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("receivedAt is required");
  }

  @Test
  void shouldStartWithReceivedStatus() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "provider",
            "evt-123",
            null,
            true,
            "{\"ok\":true}",
            Instant.parse("2026-03-30T10:15:00Z"));

    assertThat(event.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.RECEIVED);
    assertThat(event.getProcessedAt()).isNull();
  }

  @Test
  void shouldMarkEventAsProcessed() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "provider",
            "evt-123",
            null,
            true,
            "{\"ok\":true}",
            Instant.parse("2026-03-30T10:15:00Z"));
    var processedAt = Instant.parse("2026-03-30T10:20:00Z");

    event.markProcessed(processedAt);

    assertThat(event.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(event.getProcessedAt()).isEqualTo(processedAt);
  }

  @Test
  void shouldRejectNullProcessedAtWhenMarkingProcessed() {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "provider",
            "evt-123",
            null,
            true,
            "{\"ok\":true}",
            Instant.parse("2026-03-30T10:15:00Z"));

    assertThatThrownBy(() -> event.markProcessed(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("processedAt is required");
  }

  @Test
  void shouldValidateOnLifecycle() throws Exception {
    var event =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "provider",
            "evt-123",
            "PAYMENT_CONFIRMED",
            true,
            "{\"ok\":true}",
            Instant.parse("2026-03-30T10:15:00Z"));
    Method method = PaymentWebhookEvent.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);

    method.invoke(event);

    assertThat(event.getProviderName()).isEqualTo("provider");
  }

  @Test
  void shouldRejectNullProcessingStatusOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "processingStatus", null);

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("processingStatus is required");
  }

  @Test
  void shouldRejectBlankProviderNameOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "providerName", "   ");

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("providerName must not be blank");
  }

  @Test
  void shouldRejectBlankExternalEventIdOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "externalEventId", "   ");

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("externalEventId must not be blank");
  }

  @Test
  void shouldRejectBlankRawPayloadOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "rawPayload", "   ");

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("rawPayload must not be blank");
  }

  @Test
  void shouldRejectNullReceivedAtOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "receivedAt", null);

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("receivedAt is required");
  }

  @Test
  void shouldRejectProcessedStatusWithoutProcessedAtOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "processingStatus", PaymentWebhookProcessingStatus.PROCESSED);
    setField(event, "processedAt", null);

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("processedAt is required when event is processed");
  }

  @Test
  void shouldRejectReceivedStatusWithProcessedAtOnLifecycle() throws Exception {
    var event = validEvent();
    setField(event, "processingStatus", PaymentWebhookProcessingStatus.RECEIVED);
    setField(event, "processedAt", Instant.parse("2026-03-30T10:20:00Z"));

    assertThatThrownBy(() -> invokeValidateLifecycle(event))
        .hasRootCauseInstanceOf(IllegalArgumentException.class)
        .hasRootCauseMessage("processedAt must be null when event is received");
  }

  @Test
  void shouldInstantiateProtectedConstructorForJpa() throws Exception {
    Constructor<PaymentWebhookEvent> constructor =
        PaymentWebhookEvent.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    var entity = constructor.newInstance();

    assertThat(entity).isNotNull();
  }

  private PaymentWebhookEvent validEvent() {
    return new PaymentWebhookEvent(
        UUID.randomUUID(),
        null,
        "provider",
        "evt-123",
        "PAYMENT_CONFIRMED",
        true,
        "{\"ok\":true}",
        Instant.parse("2026-03-30T10:15:00Z"));
  }

  private void invokeValidateLifecycle(PaymentWebhookEvent event) throws Exception {
    Method method = PaymentWebhookEvent.class.getDeclaredMethod("validateLifecycle");
    method.setAccessible(true);
    method.invoke(event);
  }

  private void setField(PaymentWebhookEvent event, String fieldName, Object value)
      throws Exception {
    Field field = PaymentWebhookEvent.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(event, value);
  }
}
