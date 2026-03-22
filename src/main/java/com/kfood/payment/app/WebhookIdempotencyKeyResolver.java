package com.kfood.payment.app;

import com.kfood.payment.api.PaymentWebhookRequest;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class WebhookIdempotencyKeyResolver {

  public ResolvedIdempotencyKey resolve(PaymentWebhookRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    if (hasText(request.externalEventId())) {
      return new ResolvedIdempotencyKey("EXTERNAL_EVENT_ID", request.externalEventId().trim());
    }

    if (hasText(request.providerReference())) {
      return new ResolvedIdempotencyKey(
          "PROVIDER_REFERENCE_EVENT_TYPE",
          request.providerReference().trim()
              + "::"
              + request.eventType().trim().toLowerCase(Locale.ROOT));
    }

    throw new IllegalArgumentException("externalEventId or providerReference is required");
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public record ResolvedIdempotencyKey(String source, String value) {

    public ResolvedIdempotencyKey {
      if (source == null || source.isBlank()) {
        throw new IllegalArgumentException("source must not be blank");
      }

      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("value must not be blank");
      }
    }
  }
}
