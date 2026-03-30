package com.kfood.payment.app;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CashPaymentNotEnabledException extends BusinessException {

  public CashPaymentNotEnabledException(UUID storeId) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Store does not accept cash payment: " + storeId,
        HttpStatus.BAD_REQUEST);
  }
}
