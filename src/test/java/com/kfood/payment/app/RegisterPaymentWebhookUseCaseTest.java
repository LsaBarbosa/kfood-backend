package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RegisterPaymentWebhookUseCaseTest {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort =
      mock(PaymentWebhookEventPersistencePort.class);
  private final PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher =
      mock(PaymentWebhookRegisteredPublisher.class);
  private final ProcessConfirmedPaymentWebhookUseCase processConfirmedPaymentWebhookUseCase =
      mock(ProcessConfirmedPaymentWebhookUseCase.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-30T16:00:00Z"), ZoneOffset.UTC);
  private final RegisterPaymentWebhookUseCase useCase =
      new RegisterPaymentWebhookUseCase(
          paymentWebhookEventPersistencePort,
          paymentWebhookRegisteredPublisher,
          processConfirmedPaymentWebhookUseCase,
          clock);

  @Test
  void shouldRegisterNewConfirmedWebhookAndProcessItSynchronously() {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-123",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "charge-123"
            }
            """,
            true);
    var receivedEvent = receivedEvent("evt-123", "PAYMENT_CONFIRMED", true, command.rawPayload());
    var processedEvent =
        finalizedEvent(
            receivedEvent, PaymentWebhookProcessingStatus.PROCESSED, command.rawPayload());

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(true), any(), any()))
        .thenReturn(receivedEvent);
    when(processConfirmedPaymentWebhookUseCase.executeOrThrow(receivedEvent, "charge-123"))
        .thenReturn(processedEvent);

    var result = useCase.execute(command);

    assertThat(result).isSameAs(processedEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    verify(processConfirmedPaymentWebhookUseCase).executeOrThrow(receivedEvent, "charge-123");
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldReturnExistingEventWhenReplayUsesSemanticallyEquivalentPayload() {
    var existingEvent =
        finalizedEvent(
            receivedEvent(
                "evt-123",
                "PAYMENT_CONFIRMED",
                true,
                "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\",\"providerReference\":\"charge-123\"}"),
            PaymentWebhookProcessingStatus.PROCESSED,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\",\"providerReference\":\"charge-123\"}");

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock",
                """
                {
                  "providerReference": "charge-123",
                  "eventType": "PAYMENT_CONFIRMED",
                  "externalEventId": "evt-123"
                }
                """,
                true));

    assertThat(result).isSameAs(existingEvent);
    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(processConfirmedPaymentWebhookUseCase, never()).executeOrThrow(any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldNormalizeProviderReferenceBeforeSynchronousProcessing() {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-123",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "  charge-123  "
            }
            """,
            true);
    var receivedEvent = receivedEvent("evt-123", "PAYMENT_CONFIRMED", true, command.rawPayload());
    var processedEvent =
        finalizedEvent(
            receivedEvent, PaymentWebhookProcessingStatus.PROCESSED, command.rawPayload());

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(true), any(), any()))
        .thenReturn(receivedEvent);
    when(processConfirmedPaymentWebhookUseCase.executeOrThrow(receivedEvent, "charge-123"))
        .thenReturn(processedEvent);

    var result = useCase.execute(command);

    assertThat(result).isSameAs(processedEvent);
    verify(processConfirmedPaymentWebhookUseCase).executeOrThrow(receivedEvent, "charge-123");
  }

  @Test
  void shouldThrowConflictWhenReplayReusesExternalEventIdWithDifferentPayload() {
    var existingEvent =
        finalizedEvent(
            receivedEvent(
                "evt-123",
                "PAYMENT_CONFIRMED",
                true,
                "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\",\"providerReference\":\"charge-123\"}"),
            PaymentWebhookProcessingStatus.PROCESSED,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\",\"providerReference\":\"charge-123\"}");

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "externalEventId": "evt-123",
                          "eventType": "PAYMENT_CONFIRMED",
                          "providerReference": "charge-999"
                        }
                        """,
                        true)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            throwable -> {
              var businessException = (BusinessException) throwable;
              assertThat(businessException.getErrorCode())
                  .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD);
              assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(processConfirmedPaymentWebhookUseCase, never()).executeOrThrow(any(), any());
  }

  @Test
  void shouldThrowWhenStoredWebhookPayloadIsNotValidJson() {
    var existingEvent = receivedEvent("evt-123", "PAYMENT_CONFIRMED", true, "{");

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "externalEventId": "evt-123",
                          "eventType": "PAYMENT_CONFIRMED",
                          "providerReference": "charge-123"
                        }
                        """,
                        true)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Stored webhook payload is not valid JSON");

    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(processConfirmedPaymentWebhookUseCase, never()).executeOrThrow(any(), any());
  }

  @Test
  void shouldRegisterInvalidSignatureWithoutProcessing() {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-invalid",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "charge-123"
            }
            """,
            false);
    var receivedEvent =
        receivedEvent("evt-invalid", "PAYMENT_CONFIRMED", false, command.rawPayload());

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId(
            "mock", "evt-invalid"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-invalid"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenReturn(receivedEvent);

    var result = useCase.execute(command);

    assertThat(result).isSameAs(receivedEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.RECEIVED);
    verify(processConfirmedPaymentWebhookUseCase, never()).executeOrThrow(any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldProcessPaymentPendingSynchronously() {
    assertSupportedEventProcessed("PAYMENT_PENDING");
  }

  @Test
  void shouldProcessPaymentFailedSynchronously() {
    assertSupportedEventProcessed("PAYMENT_FAILED");
  }

  @Test
  void shouldProcessPaymentCanceledSynchronously() {
    assertSupportedEventProcessed("PAYMENT_CANCELED");
  }

  @Test
  void shouldProcessPaymentExpiredSynchronously() {
    assertSupportedEventProcessed("PAYMENT_EXPIRED");
  }

  @Test
  void shouldMarkUnsupportedEventTypeAsIgnored() {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-ignored",
              "eventType": "PAYMENT_REFUNDED"
            }
            """,
            true);
    var receivedEvent =
        receivedEvent("evt-ignored", "PAYMENT_REFUNDED", true, command.rawPayload());
    var ignoredEvent =
        finalizedEvent(receivedEvent, PaymentWebhookProcessingStatus.IGNORED, command.rawPayload());

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId(
            "mock", "evt-ignored"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-ignored"), eq("PAYMENT_REFUNDED"), eq(true), any(), any()))
        .thenReturn(receivedEvent);
    when(paymentWebhookEventPersistencePort.markIgnored(receivedEvent.getId(), Instant.now(clock)))
        .thenReturn(ignoredEvent);

    var result = useCase.execute(command);

    assertThat(result).isSameAs(ignoredEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.IGNORED);
    verify(processConfirmedPaymentWebhookUseCase, never()).executeOrThrow(any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldRejectMalformedJsonPayload() {
    assertThatThrownBy(() -> useCase.execute(new RegisterPaymentWebhookCommand("mock", "{", true)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Validation failed.")
        .satisfies(
            throwable ->
                assertThat(((BusinessException) throwable).getDetails())
                    .singleElement()
                    .satisfies(
                        detail -> {
                          assertThat(detail.field()).isEqualTo("body");
                          assertThat(detail.message()).isEqualTo("Malformed JSON payload.");
                        }));

    verify(paymentWebhookEventPersistencePort, never())
        .findByProviderNameAndExternalEventId(any(), any());
    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
  }

  @Test
  void shouldRejectMissingExternalEventId() {
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "eventType": "PAYMENT_CONFIRMED"
                        }
                        """,
                        true)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Validation failed.")
        .satisfies(
            throwable ->
                assertThat(((BusinessException) throwable).getDetails())
                    .singleElement()
                    .satisfies(
                        detail -> {
                          assertThat(detail.field()).isEqualTo("externalEventId");
                          assertThat(detail.message())
                              .isEqualTo("externalEventId must not be blank");
                        }));
  }

  @Test
  void shouldRejectMissingEventType() {
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "externalEventId": "evt-123"
                        }
                        """,
                        true)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Validation failed.")
        .satisfies(
            throwable ->
                assertThat(((BusinessException) throwable).getDetails())
                    .singleElement()
                    .satisfies(
                        detail -> {
                          assertThat(detail.field()).isEqualTo("eventType");
                          assertThat(detail.message()).isEqualTo("eventType must not be blank");
                        }));
  }

  @Test
  void shouldNormalizeProviderBeforeLookupAndPersistence() {
    var command =
        new RegisterPaymentWebhookCommand(
            "  mock  ",
            """
            {
              "externalEventId": "evt-123",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "charge-123"
            }
            """,
            true);
    var receivedEvent = receivedEvent("evt-123", "PAYMENT_CONFIRMED", true, command.rawPayload());
    var processedEvent =
        finalizedEvent(
            receivedEvent, PaymentWebhookProcessingStatus.PROCESSED, command.rawPayload());

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(true), any(), any()))
        .thenReturn(receivedEvent);
    when(processConfirmedPaymentWebhookUseCase.executeOrThrow(receivedEvent, "charge-123"))
        .thenReturn(processedEvent);

    var result = useCase.execute(command);

    assertThat(result.getProviderName()).isEqualTo("mock");
    verify(paymentWebhookEventPersistencePort)
        .findByProviderNameAndExternalEventId(eq("mock"), eq("evt-123"));
  }

  private void assertSupportedEventProcessed(String eventType) {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-supported",
              "eventType": "%s",
              "providerReference": "charge-123"
            }
            """
                .formatted(eventType),
            true);
    var receivedEvent = receivedEvent("evt-supported", eventType, true, command.rawPayload());
    var processedEvent =
        finalizedEvent(
            receivedEvent, PaymentWebhookProcessingStatus.PROCESSED, command.rawPayload());

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId(
            "mock", "evt-supported"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-supported"), eq(eventType), eq(true), any(), any()))
        .thenReturn(receivedEvent);
    when(processConfirmedPaymentWebhookUseCase.executeOrThrow(receivedEvent, "charge-123"))
        .thenReturn(processedEvent);

    var result = useCase.execute(command);

    assertThat(result).isSameAs(processedEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    verify(processConfirmedPaymentWebhookUseCase).executeOrThrow(receivedEvent, "charge-123");
  }

  private PaymentWebhookEvent receivedEvent(
      String externalEventId, String eventType, boolean signatureValid, String rawPayload) {
    return new PaymentWebhookEvent(
        UUID.randomUUID(),
        null,
        "mock",
        externalEventId,
        eventType,
        signatureValid,
        rawPayload,
        Instant.now(clock));
  }

  private PaymentWebhookEvent finalizedEvent(
      PaymentWebhookEvent source, PaymentWebhookProcessingStatus targetStatus, String rawPayload) {
    var event =
        new PaymentWebhookEvent(
            source.getId(),
            null,
            source.getProviderName(),
            source.getExternalEventId(),
            source.getEventType(),
            source.isSignatureValid(),
            rawPayload,
            source.getReceivedAt());
    return switch (targetStatus) {
      case PROCESSED -> {
        event.markProcessed(Instant.now(clock));
        yield event;
      }
      case IGNORED -> {
        event.markIgnored(Instant.now(clock));
        yield event;
      }
      case FAILED -> {
        event.markFailed(Instant.now(clock));
        yield event;
      }
      case RECEIVED -> event;
    };
  }
}
