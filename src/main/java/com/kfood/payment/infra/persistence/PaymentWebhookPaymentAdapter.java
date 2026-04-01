package com.kfood.payment.infra.persistence;

import com.kfood.payment.app.port.PaymentRecord;
import com.kfood.payment.app.port.PaymentWebhookPaymentPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookPaymentAdapter implements PaymentWebhookPaymentPort {

  private final PaymentRepository paymentRepository;

  public PaymentWebhookPaymentAdapter(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @Override
  public Optional<PaymentRecord> findByProviderNameAndProviderReference(
      String providerName, String providerReference) {
    return paymentRepository
        .findDetailedByProviderNameAndProviderReference(providerName, providerReference)
        .map(payment -> payment);
  }
}
