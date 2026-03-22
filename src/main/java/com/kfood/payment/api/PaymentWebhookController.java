package com.kfood.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.payment.app.PaymentWebhookReceiverService;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments/webhooks")
public class PaymentWebhookController {

  private final PaymentWebhookReceiverService paymentWebhookReceiverService;
  private final ObjectMapper objectMapper;
  private final Validator validator;

  public PaymentWebhookController(
      PaymentWebhookReceiverService paymentWebhookReceiverService, Validator validator) {
    this.paymentWebhookReceiverService = paymentWebhookReceiverService;
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.validator = validator;
  }

  @PostMapping(
      value = "/{provider}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PaymentWebhookResponse> receiveWebhook(
      @PathVariable String provider, @RequestBody(required = false) String rawPayload) {
    var request = parsePayload(rawPayload);
    validatePayload(request);

    var response = paymentWebhookReceiverService.receive(provider, request, rawPayload);
    return ResponseEntity.accepted().body(response);
  }

  private PaymentWebhookRequest parsePayload(String rawPayload) {
    if (rawPayload == null || rawPayload.isBlank()) {
      throw validationError("Invalid webhook payload.");
    }

    try {
      JsonNode root = objectMapper.readTree(rawPayload);
      return new PaymentWebhookRequest(
          textValue(root, "externalEventId"),
          textValue(root, "eventType"),
          textValue(root, "providerReference"),
          textValue(root, "paidAt"),
          decimalValue(root, "amount"));
    } catch (JsonProcessingException exception) {
      throw validationError("Invalid webhook payload.");
    }
  }

  private String textValue(JsonNode root, String fieldName) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return null;
    }

    return node.isTextual() ? node.textValue() : node.asText();
  }

  private java.math.BigDecimal decimalValue(JsonNode root, String fieldName) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return null;
    }

    return node.isNumber() ? node.decimalValue() : new java.math.BigDecimal(node.asText());
  }

  private void validatePayload(PaymentWebhookRequest request) {
    Set<jakarta.validation.ConstraintViolation<PaymentWebhookRequest>> violations =
        validator.validate(request);

    if (!violations.isEmpty()) {
      var details =
          violations.stream()
              .map(
                  violation ->
                      new com.kfood.shared.exceptions.ApiFieldError(
                          violation.getPropertyPath().toString(), violation.getMessage()))
              .toList();
      throw new BusinessException(
          ErrorCode.VALIDATION_ERROR,
          "Webhook payload has missing or invalid required fields.",
          HttpStatus.BAD_REQUEST,
          details);
    }
  }

  private BusinessException validationError(String message) {
    return new BusinessException(ErrorCode.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST);
  }
}
