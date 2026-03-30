package com.kfood.payment.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
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

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public RegisterPaymentWebhookUseCase(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort, Clock clock) {
    this.paymentWebhookEventPersistencePort = paymentWebhookEventPersistencePort;
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.clock = clock;
  }

  @Transactional
  public PaymentWebhookEventRecord execute(RegisterPaymentWebhookCommand command) {
    var normalizedProvider = command.provider().trim();
    var payload = parse(command.rawPayload());
    var externalEventId = readRequiredText(payload, "externalEventId");
    var eventType = readRequiredText(payload, "eventType");

    var existingEvent =
        paymentWebhookEventPersistencePort.findByProviderNameAndExternalEventId(
            normalizedProvider, externalEventId);
    if (existingEvent.isPresent()) {
      return existingEvent.get();
    }

    try {
      return paymentWebhookEventPersistencePort.saveReceivedEvent(
          UUID.randomUUID(),
          normalizedProvider,
          externalEventId,
          eventType,
          false,
          command.rawPayload(),
          Instant.now(clock));
    } catch (DataIntegrityViolationException exception) {
      return paymentWebhookEventPersistencePort
          .findByProviderNameAndExternalEventId(normalizedProvider, externalEventId)
          .orElseThrow(() -> exception);
    }
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
