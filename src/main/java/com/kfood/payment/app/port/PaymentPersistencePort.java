package com.kfood.payment.app.port;

import com.kfood.payment.infra.persistence.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentPersistencePort {

  Payment savePayment(Payment payment);

  Optional<Payment> findPaymentWithOrderByIdAndStoreId(UUID paymentId, UUID storeId);
}
