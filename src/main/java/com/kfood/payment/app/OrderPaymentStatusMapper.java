package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;

public final class OrderPaymentStatusMapper {

  private OrderPaymentStatusMapper() {}

  public static PaymentStatusSnapshot fromPaymentStatus(PaymentStatus paymentStatus) {
    return switch (paymentStatus) {
      case PENDING -> PaymentStatusSnapshot.PENDING;
      case CONFIRMED -> PaymentStatusSnapshot.PAID;
      case FAILED, CANCELED, EXPIRED -> PaymentStatusSnapshot.FAILED;
    };
  }
}
