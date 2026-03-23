package com.kfood.payment.app;

import com.kfood.payment.api.PaymentWebhookRequest;
import com.kfood.payment.infra.persistence.PaymentWebhookEvent;

public interface PaymentWebhookProcessor {

  void process(PaymentWebhookEvent event, PaymentWebhookRequest request);
}
