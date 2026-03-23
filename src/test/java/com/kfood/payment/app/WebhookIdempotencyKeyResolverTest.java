package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kfood.payment.api.PaymentWebhookRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WebhookIdempotencyKeyResolverTest {

  private final WebhookIdempotencyKeyResolver resolver = new WebhookIdempotencyKeyResolver();

  @Test
  void shouldPreferExternalEventIdWhenPresent() {
    var resolved =
        resolver.resolve(
            new PaymentWebhookRequest(
                " evt_001 ", "PAYMENT_CONFIRMED", "psp_ref_001", null, BigDecimal.TEN));

    assertThat(resolved.source()).isEqualTo("EXTERNAL_EVENT_ID");
    assertThat(resolved.value()).isEqualTo("evt_001");
  }

  @Test
  void shouldFallbackToProviderReferenceAndEventType() {
    var resolved =
        resolver.resolve(
            new PaymentWebhookRequest(
                null, "PAYMENT_CONFIRMED", " psp_ref_001 ", null, BigDecimal.TEN));

    assertThat(resolved.source()).isEqualTo("PROVIDER_REFERENCE_EVENT_TYPE");
    assertThat(resolved.value()).isEqualTo("psp_ref_001::payment_confirmed");
  }

  @Test
  void shouldRejectMissingIdentifiers() {
    assertThatThrownBy(
            () ->
                resolver.resolve(
                    new PaymentWebhookRequest(
                        null, "PAYMENT_CONFIRMED", null, null, BigDecimal.TEN)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("externalEventId or providerReference is required");
  }

  @Test
  void shouldRejectResolvedIdempotencyKeyWithoutSourceOrValue() {
    assertThatThrownBy(() -> new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey(" ", "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("source must not be blank");

    assertThatThrownBy(
            () -> new WebhookIdempotencyKeyResolver.ResolvedIdempotencyKey("SOURCE", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("value must not be blank");
  }
}
