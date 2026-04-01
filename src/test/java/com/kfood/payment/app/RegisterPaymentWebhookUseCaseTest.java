package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.shared.exceptions.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class RegisterPaymentWebhookUseCaseTest {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort =
      mock(PaymentWebhookEventPersistencePort.class);
  private final PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher =
      mock(PaymentWebhookRegisteredPublisher.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-03-30T16:00:00Z"), ZoneOffset.UTC);
  private final RegisterPaymentWebhookUseCase useCase =
      new RegisterPaymentWebhookUseCase(
          paymentWebhookEventPersistencePort, paymentWebhookRegisteredPublisher, clock);

  @Test
  void shouldRegisterNewWebhookEventOnFirstReceipt() {
    var command =
        new RegisterPaymentWebhookCommand(
            "mock", "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}", true);
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(true), any(), any()))
        .thenAnswer(
            invocation ->
                new PaymentWebhookEvent(
                    invocation.getArgument(0),
                    null,
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3),
                    invocation.getArgument(4),
                    invocation.getArgument(5),
                    invocation.getArgument(6)));

    var result = useCase.execute(command);

    ArgumentCaptor<UUID> eventIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<String> rawPayloadCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Instant> receivedAtCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(paymentWebhookEventPersistencePort)
        .saveReceivedEvent(
            eventIdCaptor.capture(),
            eq("mock"),
            eq("evt-123"),
            eq("PAYMENT_CONFIRMED"),
            eq(true),
            rawPayloadCaptor.capture(),
            receivedAtCaptor.capture());

    assertThat(eventIdCaptor.getValue()).isNotNull();
    assertThat(result.getId()).isEqualTo(eventIdCaptor.getValue());
    assertThat(result.getProviderName()).isEqualTo("mock");
    assertThat(result.getExternalEventId()).isEqualTo("evt-123");
    assertThat(result.getEventType()).isEqualTo("PAYMENT_CONFIRMED");
    assertThat(result.isSignatureValid()).isTrue();
    assertThat(rawPayloadCaptor.getValue()).isEqualTo(command.rawPayload());
    assertThat(result.getRawPayload()).isEqualTo(command.rawPayload());
    assertThat(receivedAtCaptor.getValue()).isEqualTo(Instant.now(clock));
    assertThat(result.getReceivedAt()).isEqualTo(Instant.now(clock));
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.RECEIVED);
    var inOrder = inOrder(paymentWebhookEventPersistencePort, paymentWebhookRegisteredPublisher);
    inOrder
        .verify(paymentWebhookEventPersistencePort)
        .saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(true), any(), any());
    inOrder
        .verify(paymentWebhookRegisteredPublisher)
        .publish(new PaymentWebhookRegisteredEvent(result.getId(), null, "PAYMENT_CONFIRMED"));
  }

  @Test
  void shouldReturnExistingEventWithoutCreatingNewOneOnReplay() {
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
    existingEvent.markProcessed(Instant.parse("2026-03-30T15:01:00Z"));

    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.of(existingEvent));

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock",
                """
                {
                  "externalEventId": "evt-123",
                  "eventType": "PAYMENT_CONFIRMED"
                }
                """));

    assertThat(result).isSameAs(existingEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    assertThat(result.getProcessedAt()).isEqualTo(Instant.parse("2026-03-30T15:01:00Z"));
    verify(paymentWebhookEventPersistencePort, never())
        .saveReceivedEvent(any(), any(), any(), any(), anyBoolean(), any(), any());
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldAcceptReplayIdempotentlyAcrossConsecutiveReceipts() {
    var firstEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.now(clock));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(firstEvent));
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenReturn(firstEvent);

    var command =
        new RegisterPaymentWebhookCommand(
            "mock", "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}");

    var firstResult = useCase.execute(command);
    var replayResult = useCase.execute(command);

    assertThat(firstResult).isSameAs(firstEvent);
    assertThat(replayResult).isSameAs(firstEvent);
    verify(paymentWebhookEventPersistencePort)
        .saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any());
    verify(paymentWebhookRegisteredPublisher)
        .publish(new PaymentWebhookRegisteredEvent(firstEvent.getId(), null, "PAYMENT_CONFIRMED"));
  }

  @Test
  void shouldNotProcessWebhookWhenEventTypeIsNotPaymentConfirmed() {
    var savedEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_PENDING",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}",
            Instant.now(clock));
    var processedEvent =
        new PaymentWebhookEvent(
            savedEvent.getId(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_PENDING",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}",
            Instant.now(clock));
    processedEvent.markProcessed(Instant.now(clock));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_PENDING"), eq(false), any(), any()))
        .thenReturn(savedEvent);
    when(paymentWebhookEventPersistencePort.markProcessed(
            savedEvent.getId(), null, Instant.now(clock)))
        .thenReturn(processedEvent);

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock", "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}"));

    assertThat(result).isSameAs(processedEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    var inOrder = inOrder(paymentWebhookEventPersistencePort, paymentWebhookRegisteredPublisher);
    inOrder
        .verify(paymentWebhookEventPersistencePort)
        .saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_PENDING"), eq(false), any(), any());
    inOrder
        .verify(paymentWebhookEventPersistencePort)
        .markProcessed(savedEvent.getId(), null, Instant.now(clock));
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldReturnSavedConfirmedEventWithoutPublishingAgainWhenStatusIsNotReceived() {
    var savedEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_CONFIRMED",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}",
            Instant.now(clock));
    savedEvent.markProcessed(Instant.now(clock));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenReturn(savedEvent);

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock", "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_CONFIRMED\"}"));

    assertThat(result).isSameAs(savedEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
    verify(paymentWebhookEventPersistencePort, never()).markProcessed(any(), any(), any());
  }

  @Test
  void shouldReturnSavedNonConfirmedEventWithoutProcessingAgainWhenStatusIsNotReceived() {
    var savedEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_PENDING",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}",
            Instant.now(clock));
    savedEvent.markProcessed(Instant.now(clock));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_PENDING"), eq(false), any(), any()))
        .thenReturn(savedEvent);

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock", "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}"));

    assertThat(result).isSameAs(savedEvent);
    assertThat(result.getProcessingStatus()).isEqualTo(PaymentWebhookProcessingStatus.PROCESSED);
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
    verify(paymentWebhookEventPersistencePort, never()).markProcessed(any(), any(), any());
  }

  @Test
  void shouldRecoverExistingEventWhenRaceConditionHitsUniqueConstraint() {
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
    existingEvent.markProcessed(Instant.parse("2026-03-30T15:01:00Z"));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenReturn(existingEvent);

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock",
                """
                {
                  "externalEventId": "evt-123",
                  "eventType": "PAYMENT_CONFIRMED"
                }
                """));

    assertThat(result).isSameAs(existingEvent);
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldFinishRecoveredNonConfirmedEventWhenRaceConditionFindsReceivedRecord() {
    var existingReceivedEvent =
        new PaymentWebhookEvent(
            UUID.randomUUID(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_PENDING",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    var processedEvent =
        new PaymentWebhookEvent(
            existingReceivedEvent.getId(),
            null,
            "mock",
            "evt-123",
            "PAYMENT_PENDING",
            false,
            "{\"externalEventId\":\"evt-123\",\"eventType\":\"PAYMENT_PENDING\"}",
            Instant.parse("2026-03-30T15:00:00Z"));
    processedEvent.markProcessed(Instant.now(clock));
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_PENDING"), eq(false), any(), any()))
        .thenReturn(existingReceivedEvent);
    when(paymentWebhookEventPersistencePort.markProcessed(
            existingReceivedEvent.getId(), null, Instant.now(clock)))
        .thenReturn(processedEvent);

    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "mock",
                """
                {
                  "externalEventId": "evt-123",
                  "eventType": "PAYMENT_PENDING"
                }
                """));

    assertThat(result).isSameAs(processedEvent);
    verify(paymentWebhookEventPersistencePort)
        .markProcessed(existingReceivedEvent.getId(), null, Instant.now(clock));
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
  }

  @Test
  void shouldPropagateUniqueConstraintFailureWhenExistingEventCannotBeRecovered() {
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "externalEventId": "evt-123",
                          "eventType": "PAYMENT_CONFIRMED"
                        }
                        """)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessage("duplicate");
  }

  @Test
  void shouldRejectMalformedJsonPayload() {
    assertThatThrownBy(() -> useCase.execute(new RegisterPaymentWebhookCommand("mock", "{")))
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
    verify(paymentWebhookRegisteredPublisher, never()).publish(any());
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
                        """)))
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
                        """)))
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
  void shouldRejectNullEventType() {
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "externalEventId": "evt-123",
                          "eventType": null
                        }
                        """)))
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
  void shouldRejectBlankExternalEventId() {
    assertThatThrownBy(
            () ->
                useCase.execute(
                    new RegisterPaymentWebhookCommand(
                        "mock",
                        """
                        {
                          "externalEventId": "   ",
                          "eventType": "PAYMENT_CONFIRMED"
                        }
                        """)))
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
  void shouldNormalizeProviderBeforeLookupAndPersistence() {
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenAnswer(
            invocation ->
                new PaymentWebhookEvent(
                    invocation.getArgument(0),
                    null,
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3),
                    invocation.getArgument(4),
                    invocation.getArgument(5),
                    invocation.getArgument(6)));
    var result =
        useCase.execute(
            new RegisterPaymentWebhookCommand(
                "  mock  ",
                """
                {
                  "externalEventId": "evt-123",
                  "eventType": "PAYMENT_CONFIRMED",
                  "providerReference": " charge-123 "
                }
                """));

    assertThat(result.getProviderName()).isEqualTo("mock");
    verify(paymentWebhookEventPersistencePort)
        .findByProviderNameAndExternalEventId(eq("mock"), eq("evt-123"));
    verify(paymentWebhookRegisteredPublisher).publish(any(PaymentWebhookRegisteredEvent.class));
  }

  @Test
  void shouldPassNullProviderReferenceWhenJsonFieldIsExplicitlyNull() {
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenAnswer(
            invocation ->
                new PaymentWebhookEvent(
                    invocation.getArgument(0),
                    null,
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3),
                    invocation.getArgument(4),
                    invocation.getArgument(5),
                    invocation.getArgument(6)));
    useCase.execute(
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-123",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": null
            }
            """));

    verify(paymentWebhookRegisteredPublisher).publish(any(PaymentWebhookRegisteredEvent.class));
  }

  @Test
  void shouldPassNullProviderReferenceWhenJsonFieldIsBlank() {
    when(paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId("mock", "evt-123"))
        .thenReturn(Optional.empty());
    when(paymentWebhookEventPersistencePort.saveReceivedEvent(
            any(), eq("mock"), eq("evt-123"), eq("PAYMENT_CONFIRMED"), eq(false), any(), any()))
        .thenAnswer(
            invocation ->
                new PaymentWebhookEvent(
                    invocation.getArgument(0),
                    null,
                    invocation.getArgument(1),
                    invocation.getArgument(2),
                    invocation.getArgument(3),
                    invocation.getArgument(4),
                    invocation.getArgument(5),
                    invocation.getArgument(6)));
    useCase.execute(
        new RegisterPaymentWebhookCommand(
            "mock",
            """
            {
              "externalEventId": "evt-123",
              "eventType": "PAYMENT_CONFIRMED",
              "providerReference": "   "
            }
            """));

    verify(paymentWebhookRegisteredPublisher).publish(any(PaymentWebhookRegisteredEvent.class));
  }
}
