package com.kfood.payment.app.port;

import com.kfood.payment.domain.PaymentMethod;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentOrder {

  UUID getId();

  UUID getStoreId();

  String getOrderNumber();

  BigDecimal getTotalAmount();

  boolean isCashPaymentEnabled();

  void markPaymentMethodSnapshot(PaymentMethod paymentMethod);

  void markPaymentStatusSnapshot(PaymentStatusSnapshot paymentStatusSnapshot);
}
