package com.kfood.payment.app;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

  @Test
  void shouldAuthenticateWithHmacSignature() throws Exception {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    provider.setMode(WebhookAuthMode.HMAC_SHA256);
    provider.setRequired(true);
    provider.setSignatureHeader("X-Signature");
    provider.setHmacSecret("test-secret");
    properties.getProviders().put("mock-psp", provider);
    var authenticator = new PaymentWebhookAuthenticator(properties);
    var headers = new HttpHeaders();
    var payload = "{\"a\":1}";
    headers.add("X-Signature", "sha256=" + hmacHex("test-secret", payload));

    assertThatCode(() -> authenticator.authenticate("mock-psp", payload, headers))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectMissingHmacSignatureAndBlankTokenHeaderName() {
    var hmacProperties = new PaymentWebhookSecurityProperties();
    var hmacProvider = new PaymentWebhookSecurityProperties.ProviderConfig();
    hmacProvider.setMode(WebhookAuthMode.HMAC_SHA256);
    hmacProvider.setRequired(true);
    hmacProvider.setSignatureHeader("X-Signature");
    hmacProvider.setHmacSecret("test-secret");
    hmacProperties.getProviders().put("mock-psp", hmacProvider);

    assertThatThrownBy(
            () ->
                new PaymentWebhookAuthenticator(hmacProperties)
                    .authenticate("mock-psp", "{}", new HttpHeaders()))
        .isInstanceOf(WebhookSignatureInvalidException.class)
        .hasMessage("Webhook signature or token is invalid.");

    var tokenProperties = new PaymentWebhookSecurityProperties();
    var tokenProvider = new PaymentWebhookSecurityProperties.ProviderConfig();
    tokenProvider.setMode(WebhookAuthMode.SHARED_TOKEN);
    tokenProvider.setRequired(true);
    tokenProvider.setTokenHeader(" ");
    tokenProvider.setSharedToken("token");
    tokenProperties.getProviders().put("mock-psp", tokenProvider);

    assertThatThrownBy(
            () ->
                new PaymentWebhookAuthenticator(tokenProperties)
                    .authenticate("mock-psp", "{}", new HttpHeaders()))
        .isInstanceOf(WebhookSignatureInvalidException.class)
        .hasMessage("Webhook signature or token is invalid.");
  }

  @Test
  void shouldIgnoreMissingProviderConfiguration() {
    var authenticator = new PaymentWebhookAuthenticator(new PaymentWebhookSecurityProperties());

    assertThatCode(() -> authenticator.authenticate("unknown-psp", "{}", new HttpHeaders()))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAuthenticateWithHmacWithoutPrefixAndNullPayload() throws Exception {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    provider.setMode(WebhookAuthMode.HMAC_SHA256);
    provider.setRequired(true);
    provider.setSignatureHeader("X-Signature");
    provider.setHmacSecret("test-secret");
    properties.getProviders().put("mock-psp", provider);
    var authenticator = new PaymentWebhookAuthenticator(properties);
    var headers = new HttpHeaders();
    headers.add("X-Signature", hmacHex("test-secret", ""));

    assertThatCode(() -> authenticator.authenticate("mock-psp", null, headers))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectSharedTokenWhenHeadersAreNull() {
    var properties = new PaymentWebhookSecurityProperties();
    var provider = new PaymentWebhookSecurityProperties.ProviderConfig();
    provider.setMode(WebhookAuthMode.SHARED_TOKEN);
    provider.setRequired(true);
    provider.setTokenHeader("X-Webhook-Token");
    provider.setSharedToken("shared-secret");
    properties.getProviders().put("mock-psp", provider);
    var authenticator = new PaymentWebhookAuthenticator(properties);

    assertThatThrownBy(() -> authenticator.authenticate("mock-psp", "{}", null))
        .isInstanceOf(WebhookSignatureInvalidException.class)
        .hasMessage("Webhook signature or token is invalid.");
  }

  @Test
  void shouldWrapHmacComputationFailures() throws Exception {
    var authenticator = new PaymentWebhookAuthenticator(new PaymentWebhookSecurityProperties());
    Method method =
        PaymentWebhookAuthenticator.class.getDeclaredMethod(
            "computeHmacHex", String.class, String.class);
    method.setAccessible(true);

    assertThatThrownBy(() -> method.invoke(authenticator, null, "{}"))
        .hasCauseInstanceOf(IllegalStateException.class)
        .cause()
        .hasMessage("Failed to compute webhook HMAC.");
  }

  private String hmacHex(String secret, String payload) throws Exception {
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
  }
}
