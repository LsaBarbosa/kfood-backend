package com.kfood.shared.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final Security security = new Security();
  private final Payment payment = new Payment();

  public Security getSecurity() {
    return security;
  }

  public Payment getPayment() {
    return payment;
  }

  public static class Security {

    @NotBlank private String jwtSecret;

    @Min(60) private long jwtExpirationSeconds;

    public String getJwtSecret() {
      return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
      this.jwtSecret = jwtSecret;
    }

    public long getJwtExpirationSeconds() {
      return jwtExpirationSeconds;
    }

    public void setJwtExpirationSeconds(long jwtExpirationSeconds) {
      this.jwtExpirationSeconds = jwtExpirationSeconds;
    }
  }

  public static class Payment {

    private final Webhook webhook = new Webhook();

    public Webhook getWebhook() {
      return webhook;
    }
  }

  public static class Webhook {

    private final Providers providers = new Providers();

    public Providers getProviders() {
      return providers;
    }
  }

  public static class Providers {

    private final Provider mock = new Provider();

    public Provider getMock() {
      return mock;
    }
  }

  public static class Provider {

    private String token;

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }
  }
}
