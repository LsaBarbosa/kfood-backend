package com.kfood.payment.app;

import com.kfood.payment.app.port.PaymentRecord;
import com.kfood.payment.app.port.PaymentWebhookEventPersistencePort;
import com.kfood.payment.app.port.PaymentWebhookEventRecord;
import com.kfood.payment.app.port.PaymentWebhookPaymentPort;
import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusTransitionException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessConfirmedPaymentWebhookUseCase {

  private final PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort;
  private final PaymentWebhookPaymentPort paymentWebhookPaymentPort;
  private final Clock clock;

  public ProcessConfirmedPaymentWebhookUseCase(
      PaymentWebhookEventPersistencePort paymentWebhookEventPersistencePort,
      PaymentWebhookPaymentPort paymentWebhookPaymentPort,
      Clock clock) {
    this.paymentWebhookEventPersistencePort = paymentWebhookEventPersistencePort;
    this.paymentWebhookPaymentPort = paymentWebhookPaymentPort;
    this.clock = clock;
  }

  @Transactional
  public PaymentWebhookEventRecord execute(
      PaymentWebhookEventRecord event, String providerReference) {
    var processedAt = Instant.now(clock);
    var normalizedProviderReference = normalizeProviderReference(providerReference);
    if (normalizedProviderReference == null) {
      return paymentWebhookEventPersistencePort.markFailedProcessing(event.getId(), processedAt);
    }

    var correlatedPayment =
        paymentWebhookPaymentPort.findByProviderNameAndProviderReference(
            event.getProviderName(), normalizedProviderReference);
    if (correlatedPayment.isEmpty()) {
      return paymentWebhookEventPersistencePort.markFailedProcessing(event.getId(), processedAt);
    }

    var payment = correlatedPayment.get();
    try {
      confirmPayment(payment, processedAt);
    } catch (PaymentStatusTransitionException exception) {
      return paymentWebhookEventPersistencePort.markFailedProcessing(event.getId(), processedAt);
    }

    return paymentWebhookEventPersistencePort.markProcessed(
        event.getId(), payment.getId(), processedAt);
  }

  private void confirmPayment(PaymentRecord payment, Instant processedAt) {
    payment.changeStatus(PaymentStatus.CONFIRMED, processedAt);
    payment
        .getOrder()
        .markPaymentStatusSnapshot(PaymentStatusSnapshotMapper.from(payment.getStatus()));
  }

  private String normalizeProviderReference(String providerReference) {
    if (providerReference == null || providerReference.isBlank()) {
      return null;
    }
    return providerReference.trim();
  }
}
