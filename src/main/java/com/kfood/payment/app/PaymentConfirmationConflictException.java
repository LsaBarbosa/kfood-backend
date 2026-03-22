package com.kfood.payment.app;

import com.kfood.payment.domain.PaymentStatus;
import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class PaymentConfirmationConflictException extends BusinessException {

  public PaymentConfirmationConflictException(PaymentStatus currentStatus) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Payment confirmation is invalid for status " + currentStatus + ".",
        HttpStatus.CONFLICT);
  }
}
