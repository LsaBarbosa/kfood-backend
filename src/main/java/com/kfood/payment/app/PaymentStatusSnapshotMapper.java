package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.payment.domain.PaymentStatusSnapshot;
import java.util.Objects;

public final class PaymentStatusSnapshotMapper {

  private PaymentStatusSnapshotMapper() {}

  public static PaymentStatusSnapshot from(PaymentStatus paymentStatus) {
    var validatedPaymentStatus =
        Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");

    return switch (validatedPaymentStatus) {
      case PENDING -> PaymentStatusSnapshot.PENDING;
      case CONFIRMED -> PaymentStatusSnapshot.PAID;
      case FAILED, CANCELED, EXPIRED -> PaymentStatusSnapshot.FAILED;
    };
  }
}
