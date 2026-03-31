package com.kfood.payment.infra.eventing;

import com.kfood.payment.app.PaymentWebhookRegisteredEvent;
import com.kfood.payment.app.PaymentWebhookRegisteredPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringPaymentWebhookRegisteredPublisher implements PaymentWebhookRegisteredPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringPaymentWebhookRegisteredPublisher(
      ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(PaymentWebhookRegisteredEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}
