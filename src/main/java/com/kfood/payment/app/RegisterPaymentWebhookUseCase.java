package com.kfood.payment.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import com.kfood.shared.exceptions.ApiFieldError;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterPaymentWebhookUseCase {

  private static final Set<String> SUPPORTED_EVENT_TYPES =
      Set.of(
          "PAYMENT_PENDING",
          "PAYMENT_CONFIRMED",
          "PAYMENT_FAILED",
          "PAYMENT_CANCELED",
          "PAYMENT_EXPIRED");

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  private final PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher;
  private final ProcessConfirmedPaymentWebhookUseCase processConfirmedPaymentWebhookUseCase;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  @Autowired
  public RegisterPaymentWebhookUseCase(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort,
      PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher,
      ObjectProvider<ProcessConfirmedPaymentWebhookUseCase> processConfirmedPaymentWebhookUseCase,
      Clock clock) {
    this(
        paymentWebhookEventPersistencePort,
        paymentWebhookRegisteredPublisher,
        processConfirmedPaymentWebhookUseCase.getIfAvailable(),
        clock);
  }

  RegisterPaymentWebhookUseCase(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort,
      PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher,
      ProcessConfirmedPaymentWebhookUseCase processConfirmedPaymentWebhookUseCase,
      Clock clock) {
    this.paymentWebhookEventPersistencePort = paymentWebhookEventPersistencePort;
    this.paymentWebhookRegisteredPublisher = paymentWebhookRegisteredPublisher;
    this.processConfirmedPaymentWebhookUseCase = processConfirmedPaymentWebhookUseCase;
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.clock = clock;
  }

  @Transactional
  public PaymentWebhookEventRecord execute(RegisterPaymentWebhookCommand command) {
    var normalizedProvider = command.provider().trim();
    var payload = parse(command.rawPayload());
    var externalEventId = readRequiredText(payload, "externalEventId");
    var eventType = readRequiredText(payload, "eventType");
    var providerReference = readOptionalText(payload, "providerReference");

    var existingEvent =
        paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId(
            normalizedProvider, externalEventId);
    if (existingEvent.isPresent()) {
      assertEquivalentPayload(existingEvent.get(), payload);
      return finalizeEvent(existingEvent.get(), command.signatureValid(), providerReference);
    }

    var savedEvent =
        paymentWebhookEventPersistencePort.saveReceivedEvent(
            UUID.randomUUID(),
            normalizedProvider,
            externalEventId,
            eventType,
            command.signatureValid(),
            command.rawPayload(),
            Instant.now(clock));
    assertEquivalentPayload(savedEvent, payload);
    return finalizeEvent(savedEvent, command.signatureValid(), providerReference);
  }

  private PaymentWebhookEventRecord finalizeEvent(
      PaymentWebhookEventRecord event, boolean signatureValid, String providerReference) {
    if (event.getProcessingStatus() != PaymentWebhookProcessingStatus.RECEIVED) {
      return event;
    }
    if (!signatureValid) {
      return event;
    }
    if (SUPPORTED_EVENT_TYPES.contains(event.getEventType())) {
      if (processConfirmedPaymentWebhookUseCase == null) {
        return paymentWebhookEventPersistencePort.markIgnored(event.getId(), Instant.now(clock));
      }
      return processConfirmedPaymentWebhookUseCase.executeOrThrow(event, providerReference);
    }
    return paymentWebhookEventPersistencePort.markIgnored(event.getId(), Instant.now(clock));
  }

  private void assertEquivalentPayload(PaymentWebhookEventRecord event, JsonNode currentPayload) {
    try {
      var existingPayload = objectMapper.readTree(event.getRawPayload());
      if (!existingPayload.equals(currentPayload)) {
        throw new BusinessException(
            ErrorCode.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD,
            "The same externalEventId was reused with a different payload.",
            HttpStatus.CONFLICT);
      }
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Stored webhook payload is not valid JSON", exception);
    }
  }

  private String readOptionalText(JsonNode payload, String fieldName) {
    var fieldNode = payload.get(fieldName);
    if (fieldNode == null || fieldNode.isNull()) {
      return null;
    }
    var value = fieldNode.asText();
    return value.isBlank() ? null : value.trim();
  }

  private JsonNode parse(String rawPayload) {
    try {
      return objectMapper.readTree(rawPayload);
    } catch (JsonProcessingException exception) {
      throw validationException(List.of(new ApiFieldError("body", "Malformed JSON payload.")));
    }
  }

  private String readRequiredText(JsonNode payload, String fieldName) {
    var fieldNode = payload.get(fieldName);
    if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
      throw validationException(
          List.of(new ApiFieldError(fieldName, fieldName + " must not be blank")));
    }
    return fieldNode.asText().trim();
  }

  private BusinessException validationException(List<ApiFieldError> details) {
    return new BusinessException(
        ErrorCode.VALIDATION_ERROR, "Validation failed.", HttpStatus.BAD_REQUEST, details);
  }
}
