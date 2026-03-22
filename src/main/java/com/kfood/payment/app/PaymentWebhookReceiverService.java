package com.kfood.payment.app;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.api.PaymentWebhookResponse;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import com.kfood.payment.infra.persistence.PaymentWebhookEventRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookReceiverService {

  private static final Logger log = LoggerFactory.getLogger(PaymentWebhookReceiverService.class);

  private final PaymentWebhookEventRepository paymentWebhookEventRepository;

  public PaymentWebhookReceiverService(
      PaymentWebhookEventRepository paymentWebhookEventRepository) {
    this.paymentWebhookEventRepository = paymentWebhookEventRepository;
  }

  @Transactional
  public PaymentWebhookResponse receive(
      String provider, PaymentWebhookRequest request, String rawPayload) {
    var normalizedProvider = normalizeProvider(provider);
    var savedEvent =
        paymentWebhookEventRepository.save(
            PaymentWebhookEvent.received(
                null, normalizedProvider, request.externalEventId(), rawPayload));

    log.info(
        "event=payment.webhook.received provider={} externalEventId={} rawPayloadStored=true",
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
}
