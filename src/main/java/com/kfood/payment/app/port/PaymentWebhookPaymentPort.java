package com.kfood.payment.app.port;

import java.util.Optional;

public interface PaymentWebhookPaymentPort {

  Optional<PaymentRecord> findByProviderNameAndProviderReference(
      String providerName, String providerReference);
}
