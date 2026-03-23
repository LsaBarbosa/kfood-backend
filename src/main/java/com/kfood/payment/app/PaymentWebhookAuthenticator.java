package com.kfood.payment.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookAuthenticator {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String INVALID_MESSAGE = "Webhook signature or token is invalid.";

  private final PaymentWebhookSecurityProperties properties;

  public PaymentWebhookAuthenticator(PaymentWebhookSecurityProperties properties) {
    this.properties = properties;
  }

  public void authenticate(String provider, String rawPayload, HttpHeaders headers) {
    var config = properties.findProvider(provider);

    if (config == null) {
      return;
    }

    if (!config.isRequired()) {
      return;
    }

    if (config.getMode() == WebhookAuthMode.NONE) {
      return;
    }

    config.validateSecrets();

    if (config.getMode() == WebhookAuthMode.HMAC_SHA256) {
      validateHmac(config, rawPayload, headers);
      return;
    }

    validateSharedToken(config, headers);
  }

  private void validateHmac(
      PaymentWebhookSecurityProperties.ProviderConfig config,
      String rawPayload,
      HttpHeaders headers) {
    var receivedSignature = firstHeader(headers, config.getSignatureHeader());

    if (receivedSignature == null || receivedSignature.isBlank()) {
      throw new WebhookSignatureInvalidException(INVALID_MESSAGE);
    }

    var expectedSignature =
        computeHmacHex(config.getHmacSecret(), rawPayload == null ? "" : rawPayload);
    var normalizedReceivedSignature = normalizeSignature(receivedSignature);

    if (!constantTimeEquals(expectedSignature, normalizedReceivedSignature)) {
      throw new WebhookSignatureInvalidException(INVALID_MESSAGE);
    }
  }

  private void validateSharedToken(
      PaymentWebhookSecurityProperties.ProviderConfig config, HttpHeaders headers) {
    var receivedToken = firstHeader(headers, config.getTokenHeader());

    if (receivedToken == null || receivedToken.isBlank()) {
      throw new WebhookSignatureInvalidException(INVALID_MESSAGE);
    }

    if (!constantTimeEquals(config.getSharedToken(), receivedToken.trim())) {
      throw new WebhookSignatureInvalidException(INVALID_MESSAGE);
    }
  }

  private String firstHeader(HttpHeaders headers, String name) {
    if (headers == null || name == null || name.isBlank()) {
      return null;
    }

    return headers.getFirst(name);
  }

  private String computeHmacHex(String secret, String payload) {
    try {
      var mac = Mac.getInstance(HMAC_ALGORITHM);
      var keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
      mac.init(keySpec);
      var digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to compute webhook HMAC.", exception);
    }
  }

  private String normalizeSignature(String value) {
    var trimmed = value.trim();
    if (trimmed.regionMatches(true, 0, "sha256=", 0, 7)) {
      return trimmed.substring(7);
    }

    return trimmed;
  }

  private boolean constantTimeEquals(String left, String right) {
    var leftBytes = left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8);
    var rightBytes = right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(leftBytes, rightBytes);
  }
}
