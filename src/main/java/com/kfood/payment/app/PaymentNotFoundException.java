package com.kfood.payment.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BusinessException {

  public PaymentNotFoundException(UUID paymentId) {
    super(
        ErrorCode.RESOURCE_NOT_FOUND,
        "Payment not found for id: " + paymentId,
        HttpStatus.NOT_FOUND);
  }
}
