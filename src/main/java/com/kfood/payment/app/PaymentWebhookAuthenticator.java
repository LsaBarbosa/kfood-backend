package com.kfood.payment.app;

public interface PaymentWebhookAuthenticator {

  boolean supports(String provider);

  void authenticate(String token);
}
