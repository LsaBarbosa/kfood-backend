package com.kfood.payment.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.api.PaymentWebhookResponse;
import com.kfood.payment.domain.WebhookProcessingStatus;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookReceiverService {

  private static final Logger log = LoggerFactory.getLogger(PaymentWebhookReceiverService.class);

  private final PaymentWebhookEventRepository paymentWebhookEventRepository;
  private final WebhookIdempotencyKeyResolver webhookIdempotencyKeyResolver;
  private final PaymentWebhookProcessor paymentWebhookProcessor;
  private final ObjectMapper objectMapper;

  public PaymentWebhookReceiverService(
      PaymentWebhookEventRepository paymentWebhookEventRepository,
      WebhookIdempotencyKeyResolver webhookIdempotencyKeyResolver,
      PaymentWebhookProcessor paymentWebhookProcessor) {
    this.paymentWebhookEventRepository = paymentWebhookEventRepository;
    this.webhookIdempotencyKeyResolver = webhookIdempotencyKeyResolver;
    this.paymentWebhookProcessor = paymentWebhookProcessor;
    objectMapper = new ObjectMapper().findAndRegisterModules();
  }

  @Transactional
  public PaymentWebhookResponse receive(
      String provider, PaymentWebhookRequest request, String rawPayload) {
    var normalizedProvider = normalizeProvider(provider);
    var resolvedKey = webhookIdempotencyKeyResolver.resolve(request);
    var existingEvent =
        paymentWebhookEventRepository.findByProviderNameAndIdempotencyKey(
            normalizedProvider, resolvedKey.value());

    if (existingEvent.isPresent()) {
      ensureSamePayload(existingEvent.get().getRawPayload(), rawPayload);

      log.info(
          "event=payment.webhook.duplicate_ignored provider={} idempotencyKey={} source={}",
          normalizedProvider,
          resolvedKey.value(),
          resolvedKey.source());

      return new PaymentWebhookResponse(
          true, WebhookProcessingStatus.IGNORED, existingEvent.get().getExternalEventId());
    }

    PaymentWebhookEvent savedEvent;
    try {
      savedEvent =
          paymentWebhookEventRepository.saveAndFlush(
              PaymentWebhookEvent.received(
                  null,
                  normalizedProvider,
                  request.externalEventId(),
                  resolvedKey.value(),
                  rawPayload));
    } catch (DataIntegrityViolationException exception) {
      var concurrentExisting =
          paymentWebhookEventRepository
              .findByProviderNameAndIdempotencyKey(normalizedProvider, resolvedKey.value())
              .orElseThrow(() -> exception);

      ensureSamePayload(concurrentExisting.getRawPayload(), rawPayload);

      return new PaymentWebhookResponse(
          true, WebhookProcessingStatus.IGNORED, concurrentExisting.getExternalEventId());
    }

    try {
      paymentWebhookProcessor.process(savedEvent, request);
      savedEvent.markProcessed();
      paymentWebhookEventRepository.save(savedEvent);
    } catch (RuntimeException exception) {
      savedEvent.markFailed();
      paymentWebhookEventRepository.save(savedEvent);
      throw exception;
    }

    log.info(
        "event=payment.webhook.processed provider={} externalEventId={} rawPayloadStored=true",
        normalizedProvider,
        request.externalEventId());

    return new PaymentWebhookResponse(
        true, savedEvent.getProcessingStatus(), savedEvent.getExternalEventId());
  }

  private String normalizeProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("provider must not be blank");
    }

    return provider.trim().toLowerCase(Locale.ROOT);
  }

  private void ensureSamePayload(String storedPayload, String incomingPayload) {
    if (!jsonEquals(storedPayload, incomingPayload)) {
      throw new IdempotencyConflictException(
          "The same idempotency key was reused with a different payload.");
    }
  }

  private boolean jsonEquals(String left, String right) {
    try {
      JsonNode leftNode = objectMapper.readTree(left);
      JsonNode rightNode = objectMapper.readTree(right);
      return Objects.equals(leftNode, rightNode);
    } catch (JsonProcessingException exception) {
      return Objects.equals(trimNullable(left), trimNullable(right));
    }
  }

  private String trimNullable(String value) {
    return value == null ? null : value.trim();
  }
}
