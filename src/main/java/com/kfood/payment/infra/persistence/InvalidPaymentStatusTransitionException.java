package com.kfood.payment.infra.persistence;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidPaymentStatusTransitionException extends BusinessException {

  public InvalidPaymentStatusTransitionException(PaymentStatus current, PaymentStatus target) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Invalid payment status transition from " + current + " to " + target,
        HttpStatus.BAD_REQUEST);
  }
}
