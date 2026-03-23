package com.kfood.payment.infra.eventing;

import com.kfood.eventing.app.OutboxCreatedEvent;
import com.kfood.eventing.app.PaymentConfirmedFacts;
import com.kfood.eventing.app.PaymentConfirmedOutboxService;
import com.kfood.payment.app.PaymentConfirmedEvent;
import com.kfood.payment.app.PaymentConfirmedPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringPaymentConfirmedPublisher implements PaymentConfirmedPublisher {

  private final PaymentConfirmedOutboxService paymentConfirmedOutboxService;
  private final ApplicationEventPublisher applicationEventPublisher;

  public SpringPaymentConfirmedPublisher(
      PaymentConfirmedOutboxService paymentConfirmedOutboxService,
      ApplicationEventPublisher applicationEventPublisher) {
    this.paymentConfirmedOutboxService = paymentConfirmedOutboxService;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publish(PaymentConfirmedEvent event) {
    var outboxEventId =
        paymentConfirmedOutboxService.enqueueOnce(
            new PaymentConfirmedFacts(
                event.paymentId().toString(),
                event.orderId().toString(),
                event.storeId().toString(),
                event.providerName(),
                event.amount(),
                event.occurredAt()));
    applicationEventPublisher.publishEvent(new OutboxCreatedEvent(outboxEventId));
  }
}
