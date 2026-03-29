package com.kfood.payment.app.port;

import com.kfood.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentPersistencePort {

  PaymentRecord savePendingPayment(
      UUID paymentId, PaymentOrder order, PaymentMethod paymentMethod, BigDecimal amount);

  default PaymentRecord savePendingPixPayment(
      UUID paymentId, PaymentOrder order, BigDecimal amount) {
    return savePendingPayment(paymentId, order, PaymentMethod.PIX, amount);
  }

  Optional<PaymentRecord> findPaymentWithOrderByIdAndStoreId(UUID paymentId, UUID storeId);
}
