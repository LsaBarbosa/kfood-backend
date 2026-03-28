package com.kfood.payment.infra.persistence;

import com.kfood.payment.domain.PaymentStatus;

public class PaymentStatusTransitionException extends RuntimeException {

  public PaymentStatusTransitionException(PaymentStatus currentStatus, PaymentStatus targetStatus) {
    super("Invalid payment status transition from " + currentStatus + " to " + targetStatus);
  }
}
