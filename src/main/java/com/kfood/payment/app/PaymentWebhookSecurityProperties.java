package com.kfood.payment.app;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.payment.webhook-security")
public class PaymentWebhookSecurityProperties {

  private Map<String, ProviderConfig> providers = new HashMap<>();

  public Map<String, ProviderConfig> getProviders() {
    return providers;
  }

  public void setProviders(Map<String, ProviderConfig> providers) {
    this.providers = providers == null ? new HashMap<>() : new HashMap<>(providers);
  }

  public ProviderConfig findProvider(String provider) {
    return providers.get(normalize(provider));
  }

  public ProviderConfig requireProvider(String provider) {
    var normalizedProvider = normalize(provider);
    var config = providers.get(normalizedProvider);

    if (config == null) {
      throw new IllegalStateException(
          "Webhook security config not found for provider " + normalizedProvider);
    }

    return config;
  }

  public static String normalize(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("provider must not be blank");
    }

    return provider.trim().toLowerCase(Locale.ROOT);
  }

  public static class ProviderConfig {

    private WebhookAuthMode mode = WebhookAuthMode.HMAC_SHA256;
    private boolean required = true;
    private String signatureHeader = "X-Signature";
    private String tokenHeader = "X-Webhook-Token";
    private String hmacSecret;
    private String sharedToken;

    public WebhookAuthMode getMode() {
      return mode;
    }

    public void setMode(WebhookAuthMode mode) {
      this.mode = mode;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }

    public String getSignatureHeader() {
      return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {
      this.signatureHeader = signatureHeader;
    }

    public String getTokenHeader() {
      return tokenHeader;
    }

    public void setTokenHeader(String tokenHeader) {
      this.tokenHeader = tokenHeader;
    }

    public String getHmacSecret() {
      return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
      this.hmacSecret = hmacSecret;
    }

    public String getSharedToken() {
      return sharedToken;
    }

    public void setSharedToken(String sharedToken) {
      this.sharedToken = sharedToken;
    }

    public void validateSecrets() {
      if (mode == null || mode == WebhookAuthMode.NONE || !required) {
        return;
      }

      if (mode == WebhookAuthMode.HMAC_SHA256 && (hmacSecret == null || hmacSecret.isBlank())) {
        throw new IllegalStateException("hmacSecret must be configured for HMAC_SHA256");
      }

      if (mode == WebhookAuthMode.SHARED_TOKEN && (sharedToken == null || sharedToken.isBlank())) {
        throw new IllegalStateException("sharedToken must be configured for SHARED_TOKEN");
      }
    }
  }
}
