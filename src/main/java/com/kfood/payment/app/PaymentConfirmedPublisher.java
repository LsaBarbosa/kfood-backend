package com.kfood.payment.app;

public interface PaymentConfirmedPublisher {

  void publish(PaymentConfirmedEvent event);
}
