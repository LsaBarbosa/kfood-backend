package com.kfood.payment.app.gateway;

import com.kfood.shared.exceptions.BusinessException;
import com.kfood.shared.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnsupportedPaymentProviderException extends BusinessException {

  public UnsupportedPaymentProviderException(String provider) {
    super(
        ErrorCode.VALIDATION_ERROR,
        "Unsupported payment provider: " + provider,
        HttpStatus.BAD_REQUEST);
  }
}
