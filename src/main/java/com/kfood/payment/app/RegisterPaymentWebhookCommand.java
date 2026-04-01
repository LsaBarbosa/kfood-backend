package com.kfood.payment.app;

public record RegisterPaymentWebhookCommand(
    String provider, String rawPayload, boolean signatureValid) {

  public RegisterPaymentWebhookCommand(String provider, String rawPayload) {
    this(provider, rawPayload, false);
  }
}
