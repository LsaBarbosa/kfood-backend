package com.kfood.payment.infra.eventing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.kfood.payment.app.PaymentWebhookRegisteredEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SpringPaymentWebhookRegisteredPublisherTest {

  private final ApplicationEventPublisher applicationEventPublisher =
      mock(ApplicationEventPublisher.class);
  private final SpringPaymentWebhookRegisteredPublisher publisher =
      new SpringPaymentWebhookRegisteredPublisher(applicationEventPublisher);

  @Test
  void shouldPublishSpringApplicationEvent() {
    var event =
        new PaymentWebhookRegisteredEvent(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "charge-123",
            "PAYMENT_CONFIRMED");

    publisher.publish(event);

    verify(applicationEventPublisher).publishEvent(event);
  }
}
