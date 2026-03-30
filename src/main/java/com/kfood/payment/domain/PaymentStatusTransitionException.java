package com.kfood.payment.domain;

public class PaymentStatusTransitionException extends RuntimeException {

  public PaymentStatusTransitionException(PaymentStatus currentStatus, PaymentStatus targetStatus) {
    super("Invalid payment status transition from " + currentStatus + " to " + targetStatus);
  }
}
