package com.kfood.payment.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectedWebhookRecorder {

  private final PaymentWebhookEventRepository repository;
  private final ObjectMapper objectMapper;

  public RejectedWebhookRecorder(PaymentWebhookEventRepository repository) {
    this.repository = repository;
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Transactional
  public void recordInvalidSignature(String provider, String rawPayload) {
    var normalizedProvider = PaymentWebhookSecurityProperties.normalize(provider);
    var storedPayload = rawPayload == null || rawPayload.isBlank() ? "{}" : rawPayload;
    var externalEventId = extractExternalEventId(storedPayload);

    var event =
        PaymentWebhookEvent.received(
            null,
            normalizedProvider,
            externalEventId,
            "auth-failed::" + UUID.randomUUID(),
            storedPayload);
    event.defineSignatureValidation(false);
    event.markFailed();

    repository.save(event);
  }

  private String extractExternalEventId(String rawPayload) {
    try {
      JsonNode root = objectMapper.readTree(rawPayload);
      JsonNode node = root.get("externalEventId");
      return node == null || node.isNull() ? null : node.asText();
    } catch (Exception exception) {
      return null;
    }
  }
}
