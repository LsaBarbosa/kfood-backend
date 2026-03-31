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
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterPaymentWebhookUseCase {

  private static final String CONFIRMED_EVENT_TYPE = "PAYMENT_CONFIRMED";

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  private final PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public RegisterPaymentWebhookUseCase(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort,
      PaymentWebhookRegisteredPublisher paymentWebhookRegisteredPublisher,
      Clock clock) {
    this.paymentWebhookEventPersistencePort = paymentWebhookEventPersistencePort;
    this.paymentWebhookRegisteredPublisher = paymentWebhookRegisteredPublisher;
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
      return existingEvent.get();
    }

    try {
      var savedEvent =
          paymentWebhookEventPersistencePort.saveReceivedEvent(
              UUID.randomUUID(),
              normalizedProvider,
              externalEventId,
              eventType,
              command.signatureValid(),
              command.rawPayload(),
              Instant.now(clock));
      if (!CONFIRMED_EVENT_TYPE.equals(eventType)) {
        return paymentWebhookEventPersistencePort.markProcessed(
            savedEvent.getId(), null, Instant.now(clock));
      }
      paymentWebhookRegisteredPublisher.publish(
          new PaymentWebhookRegisteredEvent(savedEvent.getId(), providerReference, eventType));
      return savedEvent;
    } catch (DataIntegrityViolationException exception) {
      var recoveredEvent =
          paymentWebhookEventPersistencePort
              .findByProviderNameAndExternalEventId(normalizedProvider, externalEventId)
              .orElseThrow(() -> exception);
      if (!CONFIRMED_EVENT_TYPE.equals(eventType)
          && recoveredEvent.getProcessingStatus() == PaymentWebhookProcessingStatus.RECEIVED) {
        return paymentWebhookEventPersistencePort.markProcessed(
            recoveredEvent.getId(), null, Instant.now(clock));
      }
      return recoveredEvent;
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
