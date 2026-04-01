package com.kfood.payment.app;

public interface PaymentWebhookRegisteredPublisher {

  void publish(PaymentWebhookRegisteredEvent event);
}
