package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class PaymentWebhookAuthenticatorTest {

  @Test
  void shouldAuthenticateWithSharedToken() {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    provider.setMode(WebhookAuthMode.SHARED_TOKEN);
    provider.setRequired(true);
    provider.setTokenHeader("X-Webhook-Token");
    provider.setSharedToken("shared-secret");
    properties.getProviders().put("mock-psp", provider);
    var authenticator = new PaymentWebhookAuthenticator(properties);
    var headers = new HttpHeaders();
    headers.add("X-Webhook-Token", "shared-secret");

    assertThatCode(() -> authenticator.authenticate("mock-psp", "{}", headers))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldSkipAuthenticationWhenModeIsNone() {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    provider.setMode(WebhookAuthMode.NONE);
    provider.setRequired(false);
    properties.getProviders().put("mock-psp", provider);
    var authenticator = new PaymentWebhookAuthenticator(properties);

    assertThatCode(() -> authenticator.authenticate("mock-psp", "{}", new HttpHeaders()))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectInvalidSharedToken() {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    provider.setMode(WebhookAuthMode.SHARED_TOKEN);
    provider.setRequired(true);
    provider.setTokenHeader("X-Webhook-Token");
    provider.setSharedToken("shared-secret");
    properties.getProviders().put("mock-psp", provider);
    var authenticator = new PaymentWebhookAuthenticator(properties);
    var headers = new HttpHeaders();
    headers.add("X-Webhook-Token", "wrong-token");

    assertThatThrownBy(() -> authenticator.authenticate("mock-psp", "{}", headers))
        .isInstanceOf(WebhookSignatureInvalidException.class)
        .hasMessage("Webhook signature or token is invalid.");
  }
}
