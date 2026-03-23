package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentWebhookSecurityPropertiesTest {

  @Test
  void shouldNormalizeAndResolveProviders() {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    properties.setProviders(Map.of("mock-psp", provider));

    assertThat(properties.findProvider(" mock-psp ")).isEqualTo(provider);
    assertThat(properties.requireProvider("mock-psp")).isEqualTo(provider);
    assertThat(PaymentWebhookSecurityProperties.normalize(" MOCK-PSP ")).isEqualTo("mock-psp");
  }

  @Test
  void shouldHandleNullProviderMapAndMissingProvider() {
    var properties = new PaymentWebhookSecurityProperties();
    properties.setProviders(null);

    assertThat(properties.getProviders()).isEmpty();
    assertThatThrownBy(() -> properties.requireProvider("missing"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Webhook security config not found for provider missing");
    assertThatThrownBy(() -> PaymentWebhookSecurityProperties.normalize(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("provider must not be blank");
  }

  @Test
  void shouldValidateProviderSecrets() {
    var config = new PaymentWebhookSecurityProperties.ProviderConfig();

    assertThatThrownBy(config::validateSecrets)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("hmacSecret must be configured for HMAC_SHA256");

    config.setMode(WebhookAuthMode.NONE);
    assertThatCode(config::validateSecrets).doesNotThrowAnyException();

    config.setMode(null);
    assertThatCode(config::validateSecrets).doesNotThrowAnyException();

    config.setMode(WebhookAuthMode.HMAC_SHA256);
    config.setRequired(false);
    assertThatCode(config::validateSecrets).doesNotThrowAnyException();

    config.setRequired(true);
    assertThatThrownBy(config::validateSecrets)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("hmacSecret must be configured for HMAC_SHA256");

    config.setHmacSecret("secret");
    assertThatCode(config::validateSecrets).doesNotThrowAnyException();

    config.setMode(WebhookAuthMode.SHARED_TOKEN);
    config.setSharedToken(" ");
    assertThatThrownBy(config::validateSecrets)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("sharedToken must be configured for SHARED_TOKEN");

    config.setSharedToken("token");
    config.setSignatureHeader("X-Custom-Signature");
    config.setTokenHeader("X-Custom-Token");

    assertThat(config.getMode()).isEqualTo(WebhookAuthMode.SHARED_TOKEN);
    assertThat(config.isRequired()).isTrue();
    assertThat(config.getSignatureHeader()).isEqualTo("X-Custom-Signature");
    assertThat(config.getTokenHeader()).isEqualTo("X-Custom-Token");
    assertThat(config.getHmacSecret()).isEqualTo("secret");
    assertThat(config.getSharedToken()).isEqualTo("token");
  }
}
