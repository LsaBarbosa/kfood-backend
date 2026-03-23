package com.kfood.payment.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.api.PaymentWebhookResponse;
import com.kfood.shared.exceptions.ApiFieldError;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentWebhookIngressService {

  private final PaymentWebhookAuthenticator authenticator;
  private final RejectedWebhookRecorder rejectedWebhookRecorder;
  private final PaymentWebhookReceiverService receiverService;
  private final ObjectMapper objectMapper;
  private final Validator validator;

  public PaymentWebhookIngressService(
      PaymentWebhookAuthenticator authenticator,
      RejectedWebhookRecorder rejectedWebhookRecorder,
      PaymentWebhookReceiverService receiverService,
      Validator validator) {
    this.authenticator = authenticator;
    this.rejectedWebhookRecorder = rejectedWebhookRecorder;
    this.receiverService = receiverService;
    objectMapper = new ObjectMapper().findAndRegisterModules();
    this.validator = validator;
  }

  public PaymentWebhookResponse receive(String provider, HttpHeaders headers, String rawPayload) {
    try {
      authenticator.authenticate(provider, rawPayload, headers);
    } catch (WebhookSignatureInvalidException exception) {
      rejectedWebhookRecorder.recordInvalidSignature(provider, rawPayload);
      throw exception;
    }

    var request = parsePayload(rawPayload);
    validatePayload(request);

    return receiverService.receive(provider, request, rawPayload, true);
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
    } catch (Exception exception) {
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

  private BigDecimal decimalValue(JsonNode root, String fieldName) {
    JsonNode node = root.get(fieldName);
    if (node == null || node.isNull()) {
      return null;
    }

    return node.isNumber() ? node.decimalValue() : new BigDecimal(node.asText());
  }

  private void validatePayload(PaymentWebhookRequest request) {
    Set<jakarta.validation.ConstraintViolation<PaymentWebhookRequest>> violations =
        validator.validate(request);

    if (!violations.isEmpty()) {
      var details =
          violations.stream()
              .map(
                  violation ->
                      new ApiFieldError(
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
