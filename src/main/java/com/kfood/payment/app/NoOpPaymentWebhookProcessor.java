package com.kfood.payment.app;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;
import org.springframework.stereotype.Component;

@Component
public class NoOpPaymentWebhookProcessor implements PaymentWebhookProcessor {

  @Override
  public void process(PaymentWebhookEvent event, PaymentWebhookRequest request) {
    // S13-03 closes the idempotency guard only.
    // Payment/order side effects will be implemented in S13-04.
  }
}
