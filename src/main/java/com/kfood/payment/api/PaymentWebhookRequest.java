package com.kfood.payment.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentWebhookRequest(
    String externalEventId,
    @NotBlank(message = "eventType is required") String eventType,
    String providerReference,
    String paidAt,
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "amount must be greater than or equal to zero")
        BigDecimal amount) {

  @AssertTrue(message = "externalEventId or providerReference is required") public boolean hasAtLeastOneIdempotencyIdentifier() {
    return hasText(externalEventId) || hasText(providerReference);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
