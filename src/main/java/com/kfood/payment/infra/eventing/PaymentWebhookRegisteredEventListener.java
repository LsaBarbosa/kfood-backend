package com.kfood.payment.infra.eventing;

import com.kfood.payment.app.PaymentWebhookRegisteredEvent;
import com.kfood.payment.app.ProcessConfirmedPaymentWebhookUseCase;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.domain.PaymentWebhookProcessingStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentWebhookRegisteredEventListener {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  private final ProcessConfirmedPaymentWebhookUseCase processConfirmedPaymentWebhookUseCase;

  public PaymentWebhookRegisteredEventListener(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort,
      ProcessConfirmedPaymentWebhookUseCase processConfirmedPaymentWebhookUseCase) {
    this.paymentWebhookEventPersistencePort = paymentWebhookEventPersistencePort;
    this.processConfirmedPaymentWebhookUseCase = processConfirmedPaymentWebhookUseCase;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(PaymentWebhookRegisteredEvent event) {
    paymentWebhookEventPersistencePort
        .findById(event.eventId())
        .filter(
            webhookEvent ->
                webhookEvent.getProcessingStatus() == PaymentWebhookProcessingStatus.RECEIVED)
        .ifPresent(
            webhookEvent ->
                processConfirmedPaymentWebhookUseCase.execute(
                    webhookEvent, event.providerReference()));
  }
}
